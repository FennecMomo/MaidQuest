package com.fennecmomo.maidquest.registry;

import com.fennecmomo.maidquest.MaidQuest;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// 物品注册。
public class ModItems
{
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MaidQuest.MODID);

    // 任务板方块物品
    public static final DeferredItem<BlockItem> TASK_BOARD =
            ITEMS.registerSimpleBlockItem("task_board", ModBlocks.TASK_BOARD);
}
