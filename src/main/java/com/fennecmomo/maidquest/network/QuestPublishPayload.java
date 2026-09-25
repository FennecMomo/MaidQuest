package com.fennecmomo.maidquest.network;

import com.fennecmomo.maidquest.MaidQuest;
import com.fennecmomo.maidquest.api.IQuestType;
import com.fennecmomo.maidquest.api.QuestApi;
import com.fennecmomo.maidquest.api.QuestTypeRegistry;
import com.fennecmomo.maidquest.block.entity.TaskBoardBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

// 客户端→服务端：在任务板上发布委托。
public record QuestPublishPayload(BlockPos boardPos, Identifier typeId, int quantity) implements CustomPacketPayload
{
    public static final Type<QuestPublishPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MaidQuest.MODID, "quest_publish"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestPublishPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, QuestPublishPayload::boardPos,
                    Identifier.STREAM_CODEC, QuestPublishPayload::typeId,
                    ByteBufCodecs.VAR_INT, QuestPublishPayload::quantity,
                    QuestPublishPayload::new);

    private static final double MAX_INTERACT_DISTANCE_SQ = 64.0;
    private static final int MAX_QUANTITY = 640;

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(QuestPublishPayload payload, IPayloadContext context)
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
            IQuestType type = QuestTypeRegistry.get(payload.typeId());
            if (type == null || !type.isEnabled())
            {
                return;
            }
            int quantity = Math.clamp(payload.quantity(), 1, MAX_QUANTITY);
            boolean published = QuestApi.publish(level, payload.boardPos(), type, quantity,
                    player.getName().getString(), level.getGameTime());
            if (published)
            {
                player.sendSystemMessage(Component.translatable("message.maidquest.published",
                        type.displayName(), quantity));
            }
        });
    }
}
