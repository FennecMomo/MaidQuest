package com.fennecmomo.maidquest.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

// 客户端界面打开入口（仅在客户端执行）。
public final class ClientBoardOpener
{
    private ClientBoardOpener()
    {
    }

    public static void open(BlockPos boardPos)
    {
        Minecraft.getInstance().setScreen(new QuestBoardScreen(boardPos));
    }
}
