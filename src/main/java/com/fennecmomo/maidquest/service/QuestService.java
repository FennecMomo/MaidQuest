package com.fennecmomo.maidquest.service;

import com.fennecmomo.maidquest.ModAttachments;
import com.fennecmomo.maidquest.api.IQuestExecutor;
import com.fennecmomo.maidquest.api.IQuestType;
import com.fennecmomo.maidquest.api.QuestTypeRegistry;
import com.fennecmomo.maidquest.api.QuestWeightRegistry;
import com.fennecmomo.maidquest.api.event.QuestCancelledEvent;
import com.fennecmomo.maidquest.api.event.QuestClaimedEvent;
import com.fennecmomo.maidquest.api.event.QuestCompletedEvent;
import com.fennecmomo.maidquest.api.event.QuestPublishedEvent;
import com.fennecmomo.maidquest.block.entity.TaskBoardBlockEntity;
import com.fennecmomo.maidquest.quest.Quest;
import com.fennecmomo.maidquest.quest.QuestAssignment;
import com.fennecmomo.maidquest.quest.QuestStatus;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// 委托服务：服务端唯一的发布/认领/进度/提交/放弃入口。
//
// 持久化分工：
//   委托本体 → 任务板方块实体
//   认领关系 + 进度 → 女仆 QUEST_ASSIGNMENT Attachment
//   执行器运行时状态 → 本类内存 Map（可重建，不进存档）
public final class QuestService
{
    private static final Map<UUID, IQuestExecutor> EXECUTORS = new HashMap<>();
    // 因外部约束（如背包满）暂停执行的女仆，腾出空间后自动恢复
    private static final Set<UUID> PAUSED = new HashSet<>();

    private QuestService()
    {
    }

    // ======== 查询 ========

    public static Optional<QuestAssignment> assignment(EntityMaid maid)
    {
        Optional<QuestAssignment> value = maid.getExistingDataOrNull(ModAttachments.QUEST_ASSIGNMENT);
        if (value == null || value.isEmpty())
        {
            return Optional.empty();
        }
        Quest quest = value.get().quest();
        if (maid.level() instanceof ServerLevel level && level.isLoaded(quest.boardPos())
                && level.getBlockEntity(quest.boardPos()) instanceof TaskBoardBlockEntity board)
        {
            Quest current = board.findQuest(quest.id());
            if (current == null || (current.status() != QuestStatus.CLAIMED
                    && current.status() != QuestStatus.IN_PROGRESS)
                    || !current.claimedBy().filter(maid.getUUID()::equals).isPresent())
            {
                clearAssignment(maid);
                releaseExecutor(maid);
                return Optional.empty();
            }
        }
        return value;
    }

    public static Optional<Quest> currentQuest(EntityMaid maid)
    {
        return assignment(maid).map(QuestAssignment::quest);
    }

    public static int progress(EntityMaid maid)
    {
        return assignment(maid).map(QuestAssignment::progress).orElse(0);
    }

    public static List<Quest> questsAt(ServerLevel level, BlockPos boardPos)
    {
        if (level.getBlockEntity(boardPos) instanceof TaskBoardBlockEntity board)
        {
            return board.getQuests();
        }
        return List.of();
    }

    // ======== 发布 / 取消 ========

    public static boolean publish(ServerLevel level, BlockPos boardPos, Quest quest)
    {
        if (!(level.getBlockEntity(boardPos) instanceof TaskBoardBlockEntity board))
        {
            return false;
        }
        board.addQuest(quest);
        NeoForge.EVENT_BUS.post(new QuestPublishedEvent(level, quest));
        return true;
    }

    // 仅发布者本人可撤销未完成/未取消的委托。
    public static boolean cancel(ServerLevel level, BlockPos boardPos, UUID questId, String requester)
    {
        if (!(level.getBlockEntity(boardPos) instanceof TaskBoardBlockEntity board))
        {
            return false;
        }
        Quest quest = board.findQuest(questId);
        if (quest == null || quest.status() == QuestStatus.COMPLETED || quest.status() == QuestStatus.CANCELLED)
        {
            return false;
        }
        if (!quest.postedBy().equals(requester))
        {
            return false;
        }
        Quest cancelled = quest.cancelled();
        board.updateQuest(cancelled);
        quest.claimedBy().ifPresent(maidUuid ->
        {
            EntityMaid maid = findMaid(level, maidUuid);
            if (maid != null)
            {
                clearAssignment(maid);
                releaseExecutor(maid);
            }
        });
        NeoForge.EVENT_BUS.post(new QuestCancelledEvent(level, cancelled, requester));
        return true;
    }

    // ======== 认领 ========

    public static boolean claim(ServerLevel level, EntityMaid maid, BlockPos boardPos, UUID questId)
    {
        if (assignment(maid).isPresent())
        {
            return false;
        }
        if (!(level.getBlockEntity(boardPos) instanceof TaskBoardBlockEntity board))
        {
            return false;
        }
        Quest quest = board.findQuest(questId);
        if (quest == null || quest.status() != QuestStatus.OPEN)
        {
            return false;
        }
        if (QuestWeightRegistry.totalWeight(maid, quest) <= 0)
        {
            return false;
        }
        Quest claimed = quest.claimed(maid.getUUID(), maid.getName().getString());
        board.updateQuest(claimed);
        setAssignment(maid, new QuestAssignment(claimed, 0));
        NeoForge.EVENT_BUS.post(new QuestClaimedEvent(level, claimed, maid));
        return true;
    }

    // ======== 执行 ========

    // 取当前执行器；不存在则创建。首次开工时把委托标记为执行中。
    @Nullable
    public static IQuestExecutor ensureExecutor(ServerLevel level, EntityMaid maid)
    {
        Optional<QuestAssignment> optional = assignment(maid);
        if (optional.isEmpty())
        {
            return null;
        }
        IQuestExecutor executor = EXECUTORS.get(maid.getUUID());
        if (executor != null)
        {
            return executor;
        }
        QuestAssignment assignment = optional.get();
        Quest quest = assignment.quest();
        IQuestType type = QuestTypeRegistry.get(quest.typeId());
        if (type == null)
        {
            // 类型已不存在（外部模组移除）→ 清理认领，避免卡死
            clearAssignment(maid);
            return null;
        }
        if (quest.status() == QuestStatus.CLAIMED)
        {
            quest = quest.started();
            setAssignment(maid, assignment.withQuest(quest));
            updateBoardQuest(level, quest);
        }
        executor = type.createExecutor(quest, maid, level);
        EXECUTORS.put(maid.getUUID(), executor);
        return executor;
    }

    public static void releaseExecutor(EntityMaid maid)
    {
        IQuestExecutor executor = EXECUTORS.remove(maid.getUUID());
        if (executor != null && maid.level() instanceof ServerLevel level)
        {
            executor.stop(maid, level);
        }
    }

    public static void addProgress(EntityMaid maid, int delta)
    {
        assignment(maid).ifPresent(assignment ->
        {
            int progress = Math.min(assignment.progress() + delta, assignment.quest().quantity());
            setAssignment(maid, assignment.withProgress(progress));
        });
    }

    // ======== 暂停 / 恢复（外部约束，如背包满） ========

    public static boolean isPaused(EntityMaid maid)
    {
        return PAUSED.contains(maid.getUUID());
    }

    public static void pause(EntityMaid maid)
    {
        if (PAUSED.add(maid.getUUID()))
        {
            QuestFeedback.bubble(maid, "bubble.maidquest.inventory_full");
        }
    }

    public static void resume(EntityMaid maid)
    {
        PAUSED.remove(maid.getUUID());
    }

    // ======== 提交 ========

    // 是否该回任务板提交：交付型 = 身上有货且（凑齐或背包已满）；计数型 = 进度达标。
    public static boolean isReadyToSubmit(EntityMaid maid)
    {
        Optional<QuestAssignment> optional = assignment(maid);
        if (optional.isEmpty())
        {
            return false;
        }
        QuestAssignment assignment = optional.get();
        Quest quest = assignment.quest();
        IQuestType type = QuestTypeRegistry.get(quest.typeId());
        if (type == null)
        {
            return false;
        }
        if (type.requiresDelivery())
        {
            int carried = type.countCarried(quest, maid);
            return carried > 0 && (assignment.progress() + carried >= quest.quantity() || isInventoryFull(maid));
        }
        return assignment.progress() >= quest.quantity();
    }

    // 结算：交付型先把产物存入任务板仓库，凑齐则完成；未凑齐保留进度回去继续。
    public static boolean submit(ServerLevel level, EntityMaid maid)
    {
        Optional<QuestAssignment> optional = assignment(maid);
        if (optional.isEmpty())
        {
            return false;
        }
        QuestAssignment assignment = optional.get();
        Quest quest = assignment.quest();
        IQuestType type = QuestTypeRegistry.get(quest.typeId());
        if (type == null)
        {
            clearAssignment(maid);
            releaseExecutor(maid);
            return false;
        }
        if (!(level.getBlockEntity(quest.boardPos()) instanceof TaskBoardBlockEntity board))
        {
            return false;
        }
        int progress = assignment.progress();
        if (type.requiresDelivery())
        {
            progress += type.deposit(quest, maid, board.getStorage(),
                    Math.max(0, quest.quantity() - progress));
        }
        if (progress >= quest.quantity())
        {
            Quest completed = quest.completed(level.getGameTime());
            board.updateQuest(completed);
            clearAssignment(maid);
            releaseExecutor(maid);
            QuestFeedback.playDeliver(maid);
            NeoForge.EVENT_BUS.post(new QuestCompletedEvent(level, completed, maid));
            return true;
        }
        setAssignment(maid, assignment.withProgress(progress));
        // 身上已无可交付物 → 释放执行器，重新进入执行阶段
        if (type.countCarried(quest, maid) <= 0)
        {
            releaseExecutor(maid);
        }
        return true;
    }

    // ======== 放弃 ========

    // 女仆侧放弃：reopen = true 时委托退回待接取，否则保持原状态（如已被发布者取消）。
    public static void abandon(ServerLevel level, EntityMaid maid, boolean reopen)
    {
        Optional<QuestAssignment> optional = assignment(maid);
        if (optional.isEmpty())
        {
            return;
        }
        Quest quest = optional.get().quest();
        clearAssignment(maid);
        releaseExecutor(maid);
        if (reopen && level.getBlockEntity(quest.boardPos()) instanceof TaskBoardBlockEntity board)
        {
            Quest current = board.findQuest(quest.id());
            if (current != null && current.status() != QuestStatus.COMPLETED && current.status() != QuestStatus.CANCELLED)
            {
                board.updateQuest(current.reopened());
            }
        }
    }

    // ======== 工具 ========

    // 女仆可接掉落物的背包是否已无空格。
    // 必须与 TLM dropResourcesToMaidInv 使用同一个 handler（getAvailableInv(false)）。
    public static boolean isInventoryFull(EntityMaid maid)
    {
        CombinedResourceHandler<ItemResource> inv = maid.getItemManager().getAvailableInv(false);
        for (int i = 0; i < inv.size(); i++)
        {
            if (inv.getResource(i).isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    // 服务器停止时清理运行时状态。
    public static void clearAll()
    {
        EXECUTORS.clear();
        PAUSED.clear();
    }

    private static void setAssignment(EntityMaid maid, QuestAssignment assignment)
    {
        maid.setData(ModAttachments.QUEST_ASSIGNMENT, Optional.of(assignment));
    }

    private static void clearAssignment(EntityMaid maid)
    {
        maid.setData(ModAttachments.QUEST_ASSIGNMENT, Optional.empty());
        PAUSED.remove(maid.getUUID());
    }

    private static void updateBoardQuest(ServerLevel level, Quest quest)
    {
        if (level.getBlockEntity(quest.boardPos()) instanceof TaskBoardBlockEntity board)
        {
            board.updateQuest(quest);
        }
    }

    @Nullable
    private static EntityMaid findMaid(ServerLevel level, UUID uuid)
    {
        return level.getEntity(uuid) instanceof EntityMaid maid ? maid : null;
    }
}
