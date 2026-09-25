package com.fennecmomo.maidquest.service;

import com.fennecmomo.maidquest.api.IQuestType;
import com.fennecmomo.maidquest.api.QuestTypeRegistry;
import com.fennecmomo.maidquest.api.QuestWeightRegistry;
import com.fennecmomo.maidquest.block.entity.TaskBoardBlockEntity;
import com.fennecmomo.maidquest.quest.Quest;
import com.fennecmomo.maidquest.quest.QuestStatus;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// 委托匹配工具：可接委托筛选与加权抽取。
public final class QuestMatcher
{
    private QuestMatcher()
    {
    }

    // 范围内最近的有可接委托的任务板，没有返回 null。
    @Nullable
    public static BlockPos findBoardWithClaimableQuest(ServerLevel level, EntityMaid maid, double radius)
    {
        for (BlockPos pos : QuestBoardRegistry.boardsNear(level, maid.blockPosition(), radius))
        {
            if (!claimableQuests(level, pos, maid).isEmpty())
            {
                return pos;
            }
        }
        return null;
    }

    // 任务板上该女仆可接的委托（待接取 + 权重 > 0）。
    public static List<Quest> claimableQuests(ServerLevel level, BlockPos boardPos, EntityMaid maid)
    {
        if (!(level.getBlockEntity(boardPos) instanceof TaskBoardBlockEntity board))
        {
            return List.of();
        }
        List<Quest> result = new ArrayList<>();
        for (Quest quest : board.getQuests())
        {
            if (quest.status() != QuestStatus.OPEN)
            {
                continue;
            }
            IQuestType type = QuestTypeRegistry.get(quest.typeId());
            if (type == null || !type.isEnabled())
            {
                continue;
            }
            if (QuestWeightRegistry.totalWeight(maid, quest) > 0)
            {
                result.add(quest);
            }
        }
        return result;
    }

    // 按匹配度加权随机抽一个委托。
    @Nullable
    public static Quest pickWeighted(EntityMaid maid, List<Quest> quests)
    {
        int totalWeight = 0;
        for (Quest quest : quests)
        {
            totalWeight += QuestWeightRegistry.totalWeight(maid, quest);
        }
        if (totalWeight <= 0)
        {
            return null;
        }
        int roll = maid.getRandom().nextInt(totalWeight);
        for (Quest quest : quests)
        {
            roll -= QuestWeightRegistry.totalWeight(maid, quest);
            if (roll < 0)
            {
                return quest;
            }
        }
        return quests.get(0);
    }
}
