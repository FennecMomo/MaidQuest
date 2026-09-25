package com.fennecmomo.maidquest.registry;

import com.fennecmomo.maidquest.MaidQuest;
import com.fennecmomo.maidquest.block.TaskBoardBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

// 方块注册。
public class ModBlocks
{
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MaidQuest.MODID);

    // 任务板——贴墙放置，右键打开委托界面
    public static final DeferredBlock<TaskBoardBlock> TASK_BOARD =
            BLOCKS.register("task_board", id -> new TaskBoardBlock(Block.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .mapColor(MapColor.WOOD)
                    .strength(1.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));
}
