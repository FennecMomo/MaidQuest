package com.fennecmomo.maidquest.api;

import com.fennecmomo.maidquest.quest.Quest;
import com.fennecmomo.maidquest.service.QuestBoardRegistry;
import com.fennecmomo.maidquest.service.QuestService;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// 女仆委托对外入口。
//
// 其他模组通过本类发布/查询/取消委托；女仆侧由 maidquest 的 AI 行为自动接取执行。
public final class QuestApi
{
    private QuestApi()
    {
    }

    // ======== 发布 ========

    // 按类型默认参数发布委托。
    public static boolean publish(ServerLevel level, BlockPos boardPos, IQuestType type,
                                  int quantity, String postedBy, long gameTime)
    {
        Quest quest = type.create(UUID.randomUUID(), postedBy, quantity, boardPos, gameTime);
        return publish(level, boardPos, quest);
    }

    // 发布一个已构造好的委托。
    public static boolean publish(ServerLevel level, BlockPos boardPos, Quest quest)
    {
        return QuestService.publish(level, boardPos, quest);
    }

    // ======== 查询 ========

    public static List<Quest> questsAt(ServerLevel level, BlockPos boardPos)
    {
        return QuestService.questsAt(level, boardPos);
    }

    // 女仆当前认领的委托。
    public static Optional<Quest> currentQuest(EntityMaid maid)
    {
        return QuestService.currentQuest(maid);
    }

    // 范围内的任务板位置（按距离排序）。
    public static List<BlockPos> boardsNear(ServerLevel level, BlockPos pos, double radius)
    {
        return QuestBoardRegistry.boardsNear(level, pos, radius);
    }

    // ======== 取消 ========

    // 仅发布者本人可撤销自己的委托。
    public static boolean cancel(ServerLevel level, BlockPos boardPos, UUID questId, String requester)
    {
        return QuestService.cancel(level, boardPos, questId, requester);
    }
}
