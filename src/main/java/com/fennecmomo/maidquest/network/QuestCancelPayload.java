package com.fennecmomo.maidquest.network;

import com.fennecmomo.maidquest.MaidQuest;
import com.fennecmomo.maidquest.api.QuestApi;
import com.fennecmomo.maidquest.block.entity.TaskBoardBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

// 客户端→服务端：撤销任务板上的委托（仅发布者本人）。
public record QuestCancelPayload(BlockPos boardPos, UUID questId) implements CustomPacketPayload
{
    public static final Type<QuestCancelPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MaidQuest.MODID, "quest_cancel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestCancelPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, QuestCancelPayload::boardPos,
                    UUIDUtil.STREAM_CODEC, QuestCancelPayload::questId,
                    QuestCancelPayload::new);

    private static final double MAX_INTERACT_DISTANCE_SQ = 64.0;

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(QuestCancelPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (!(context.player() instanceof ServerPlayer player))
            {
                return;
            }
            ServerLevel level = player.level();
            if (player.blockPosition().distSqr(payload.boardPos()) > MAX_INTERACT_DISTANCE_SQ)
            {
                return;
            }
            if (!(level.getBlockEntity(payload.boardPos()) instanceof TaskBoardBlockEntity))
            {
                return;
            }
            boolean cancelled = QuestApi.cancel(level, payload.boardPos(), payload.questId(),
                    player.getName().getString());
            if (cancelled)
            {
                player.sendSystemMessage(Component.translatable("message.maidquest.cancelled"));
            }
            else
            {
                player.sendSystemMessage(Component.translatable("message.maidquest.cancel_denied"));
            }
        });
    }
}
