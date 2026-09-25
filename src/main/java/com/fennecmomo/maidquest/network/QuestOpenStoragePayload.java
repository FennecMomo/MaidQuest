package com.fennecmomo.maidquest.network;

import com.fennecmomo.maidquest.MaidQuest;
import com.fennecmomo.maidquest.block.entity.TaskBoardBlockEntity;
import com.fennecmomo.maidquest.menu.TaskBoardMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

// 客户端→服务端：打开任务板的交付仓库。
public record QuestOpenStoragePayload(BlockPos boardPos) implements CustomPacketPayload
{
    public static final Type<QuestOpenStoragePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MaidQuest.MODID, "quest_open_storage"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestOpenStoragePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, QuestOpenStoragePayload::boardPos,
                    QuestOpenStoragePayload::new);

    private static final double MAX_INTERACT_DISTANCE_SQ = 64.0;

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(QuestOpenStoragePayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (!(context.player() instanceof ServerPlayer player))
            {
                return;
            }
            if (player.blockPosition().distSqr(payload.boardPos()) > MAX_INTERACT_DISTANCE_SQ)
            {
                return;
            }
            if (!(player.level().getBlockEntity(payload.boardPos()) instanceof TaskBoardBlockEntity board))
            {
                return;
            }
            BlockPos pos = board.getBlockPos();
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new TaskBoardMenu(id, inventory, board),
                    Component.translatable("menu.maidquest.task_board")), buffer -> buffer.writeBlockPos(pos));
        });
    }
}
