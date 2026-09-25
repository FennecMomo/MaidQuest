package com.fennecmomo.maidquest.service;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// 任务板位置查询入口。
public final class QuestBoardRegistry
{
    private QuestBoardRegistry()
    {
    }

    public static void register(ServerLevel level, BlockPos pos)
    {
        level.getDataStorage().computeIfAbsent(QuestBoardData.TYPE).add(pos);
    }

    public static void unregister(ServerLevel level, BlockPos pos)
    {
        level.getDataStorage().computeIfAbsent(QuestBoardData.TYPE).remove(pos);
    }

    // 返回半径内已加载的任务板位置，按距离升序。
    public static List<BlockPos> boardsNear(ServerLevel level, BlockPos from, double radius)
    {
        double radiusSq = radius * radius;
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : level.getDataStorage().computeIfAbsent(QuestBoardData.TYPE).snapshot())
        {
            if (pos.distSqr(from) <= radiusSq && level.isLoaded(pos))
            {
                result.add(pos);
            }
        }
        result.sort(Comparator.comparingDouble(pos -> pos.distSqr(from)));
        return result;
    }
}
