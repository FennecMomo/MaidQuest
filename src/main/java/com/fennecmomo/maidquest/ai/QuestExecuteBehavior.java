package com.fennecmomo.maidquest.ai;

import com.fennecmomo.fenneclib.behavior.WeightedChildBehavior;
import com.fennecmomo.maidquest.MaidQuestConfig;
import com.fennecmomo.maidquest.api.IQuestExecutor;
import com.fennecmomo.maidquest.block.entity.TaskBoardBlockEntity;
import com.fennecmomo.maidquest.quest.QuestAssignment;
import com.fennecmomo.maidquest.service.QuestFeedback;
import com.fennecmomo.maidquest.service.QuestService;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

// 执行行为。
//
// 女仆有认领中的委托且未到提交条件时，驱动委托类型的执行器（砍树/挖矿等）。
// 执行器由 QuestService 管理，行为本身不持有跨阶段状态。
public class QuestExecuteBehavior extends WeightedChildBehavior
{
    @Nullable
    private IQuestExecutor executor;
    private boolean finished;

    public QuestExecuteBehavior()
    {
        super(Map.of(), MaidQuestConfig.EXECUTOR_MAX_DURATION);
    }

    // ======== 权重与条件 ========

    @Override
    public int getWeight(EntityMaid maid)
    {
        if (QuestService.assignment(maid).isEmpty() || QuestService.isReadyToSubmit(maid))
        {
            return 0;
        }
        return MaidQuestConfig.EXECUTE_WEIGHT;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid)
    {
        tryResume(maid);
        if (QuestService.isPaused(maid))
        {
            return false;
        }
        return QuestService.assignment(maid).isPresent() && !QuestService.isReadyToSubmit(maid);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long time)
    {
        if (finished)
        {
            return false;
        }
        // 委托板被拆除 → 放弃委托，避免无限执行
        if (boardMissing(level, maid))
        {
            QuestFeedback.bubble(maid, "bubble.maidquest.board_lost");
            QuestService.abandon(level, maid, false);
            finished = true;
            return false;
        }
        tryResume(maid);
        if (QuestService.isPaused(maid))
        {
            return false;
        }
        return QuestService.assignment(maid).isPresent() && !QuestService.isReadyToSubmit(maid);
    }

    // ======== 生命周期 ========

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long time)
    {
        finished = false;
        executor = QuestService.ensureExecutor(level, maid);
        if (executor == null)
        {
            finished = true;
        }
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long time)
    {
        if (executor == null)
        {
            finished = true;
            return;
        }
        if (!executor.tick(maid, level))
        {
            finished = true;
            // 因外部约束停住（如背包塞不下）且手上没有可交付物 → 暂停，腾出空间后自动恢复；
            // 手上有货则交给提交行为先去仓库卸货
            if (executor.isBlocked() && !QuestService.isReadyToSubmit(maid))
            {
                QuestService.pause(maid);
            }
        }
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long time)
    {
        // 执行器由 QuestService 持有，阶段结束不销毁，提交/放弃时由服务释放
        executor = null;
    }

    // 暂停后背包腾出空位 → 自动恢复
    private void tryResume(EntityMaid maid)
    {
        if (QuestService.isPaused(maid) && !QuestService.isInventoryFull(maid))
        {
            QuestService.resume(maid);
        }
    }

    // 委托板所在区块已加载但方块实体不存在（被拆除）
    private boolean boardMissing(ServerLevel level, EntityMaid maid)
    {
        Optional<QuestAssignment> optional = QuestService.assignment(maid);
        if (optional.isEmpty())
        {
            return false;
        }
        BlockPos boardPos = optional.get().quest().boardPos();
        return level.isLoaded(boardPos) && !(level.getBlockEntity(boardPos) instanceof TaskBoardBlockEntity);
    }
}
