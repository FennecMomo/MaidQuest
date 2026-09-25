package com.fennecmomo.maidquest.menu;

import com.fennecmomo.fenneclib.template.Data.GenericContainerMenu;
import com.fennecmomo.maidquest.block.entity.TaskBoardBlockEntity;
import com.fennecmomo.maidquest.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

// 任务板交付仓库菜单（9×6）。
public class TaskBoardMenu extends GenericContainerMenu
{
    public static final int COLS = TaskBoardBlockEntity.STORAGE_COLS;
    public static final int ROWS = TaskBoardBlockEntity.STORAGE_ROWS;

    private static final double MAX_USE_DISTANCE_SQ = 64.0;

    // 服务端为真实板子位置；客户端由打开菜单的额外数据提供
    private final BlockPos boardPos;

    // 服务端构造
    public TaskBoardMenu(int id, Inventory playerInventory, TaskBoardBlockEntity board)
    {
        super(ModMenus.TASK_BOARD.get(), id, COLS, ROWS, board.getStorage(), playerInventory);
        this.boardPos = board.getBlockPos();
    }

    // 客户端构造（IMenuTypeExtension 读取额外数据）
    public TaskBoardMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf data)
    {
        this(id, playerInventory, data.readBlockPos());
    }

    private TaskBoardMenu(int id, Inventory playerInventory, BlockPos boardPos)
    {
        super(ModMenus.TASK_BOARD.get(), id, COLS, ROWS, new SimpleContainer(COLS * ROWS), playerInventory);
        this.boardPos = boardPos;
    }

    public BlockPos getBoardPos()
    {
        return boardPos;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return player.distanceToSqr(Vec3.atCenterOf(boardPos)) <= MAX_USE_DISTANCE_SQ;
    }
}
