package com.fennecmomo.maidquest.registry;

import com.fennecmomo.maidquest.MaidQuest;
import com.fennecmomo.maidquest.menu.TaskBoardMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// 菜单注册。
public class ModMenus
{
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MaidQuest.MODID);

    // 任务板交付仓库：客户端从打开菜单的额外数据里读任务板位置（用于「返回」）
    public static final Supplier<MenuType<TaskBoardMenu>> TASK_BOARD =
            MENUS.register("task_board", () -> IMenuTypeExtension.create(
                    (containerId, inventory, data) -> new TaskBoardMenu(containerId, inventory, data)));
}
