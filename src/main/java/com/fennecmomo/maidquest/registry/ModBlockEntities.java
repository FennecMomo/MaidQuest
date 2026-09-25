package com.fennecmomo.maidquest.registry;

import com.fennecmomo.maidquest.MaidQuest;
import com.fennecmomo.maidquest.block.entity.TaskBoardBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// 方块实体注册。
public class ModBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MaidQuest.MODID);

    public static final Supplier<BlockEntityType<TaskBoardBlockEntity>> TASK_BOARD =
            BLOCK_ENTITIES.register("task_board",
                    () -> new BlockEntityType<>(TaskBoardBlockEntity::new, ModBlocks.TASK_BOARD.get()));
}
