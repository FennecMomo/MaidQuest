package com.fennecmomo.maidquest.registry;

import com.fennecmomo.maidquest.MaidQuest;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// 创造模式标签页注册。
public class ModCreativeTabs
{
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MaidQuest.MODID);

    public static final Supplier<CreativeModeTab> MAIDQUEST_TAB = TABS.register("maidquest",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.maidquest"))
                    .icon(() -> new ItemStack(ModItems.TASK_BOARD.get()))
                    .displayItems((parameters, output) -> output.accept(ModItems.TASK_BOARD.get()))
                    .build());
}
