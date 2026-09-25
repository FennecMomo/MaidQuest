package com.fennecmomo.maidquest.service;

import com.fennecmomo.maidquest.MaidQuest;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 任务板位置登记（SavedData，每维度一份）。
//
// 女仆找板子时直接查登记表，避免方块扫描；任务板方块实体加载时登记、移除时注销。
public class QuestBoardData extends SavedData
{
    public static final Codec<QuestBoardData> CODEC =
            BlockPos.CODEC.listOf().xmap(QuestBoardData::new, QuestBoardData::snapshot);

    public static final SavedDataType<QuestBoardData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(MaidQuest.MODID, "boards"),
            QuestBoardData::new,
            CODEC);

    private final Set<BlockPos> boards = new HashSet<>();

    public QuestBoardData()
    {
    }

    private QuestBoardData(List<BlockPos> list)
    {
        boards.addAll(list);
    }

    public void add(BlockPos pos)
    {
        if (boards.add(pos.immutable()))
        {
            setDirty();
        }
    }

    public void remove(BlockPos pos)
    {
        if (boards.remove(pos))
        {
            setDirty();
        }
    }

    public List<BlockPos> snapshot()
    {
        return List.copyOf(boards);
    }
}
