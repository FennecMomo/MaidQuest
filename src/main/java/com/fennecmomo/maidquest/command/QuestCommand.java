package com.fennecmomo.maidquest.command;

import com.fennecmomo.maidquest.api.IQuestType;
import com.fennecmomo.maidquest.api.QuestApi;
import com.fennecmomo.maidquest.api.QuestTypeRegistry;
import com.fennecmomo.maidquest.quest.Quest;
import com.fennecmomo.maidquest.quest.QuestStatus;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

// /maidquest 命令。
//
//   /maidquest publish <type> <quantity>   在最近的任务板发布委托
//   /maidquest list                        查看最近任务板上的委托
//   /maidquest boards                      查看已登记的任务板（调试）
public final class QuestCommand
{
    private static final double BOARD_COMMAND_RADIUS = 64.0;
    private static final int MAX_QUANTITY = 640;

    private QuestCommand()
    {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("maidquest")
                .then(Commands.literal("publish")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((context, builder) ->
                                {
                                    for (IQuestType type : QuestTypeRegistry.getEnabled())
                                    {
                                        builder.suggest(type.id().getPath());
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("quantity", IntegerArgumentType.integer(1, MAX_QUANTITY))
                                        .executes(context -> publish(context.getSource(),
                                                StringArgumentType.getString(context, "type"),
                                                IntegerArgumentType.getInteger(context, "quantity"))))))
                .then(Commands.literal("list")
                        .executes(context -> list(context.getSource())))
                .then(Commands.literal("boards")
                        .executes(context -> boards(context.getSource()))));
    }

    private static int publish(CommandSourceStack source, String typeName, int quantity)
    {
        ServerPlayer player = source.getPlayer();
        if (player == null)
        {
            source.sendFailure(Component.translatable("command.maidquest.player_only"));
            return 0;
        }
        IQuestType type = resolveType(typeName);
        if (type == null)
        {
            source.sendFailure(Component.translatable("command.maidquest.unknown_type", typeName));
            return 0;
        }
        BlockPos boardPos = nearestBoard(player);
        if (boardPos == null)
        {
            source.sendFailure(Component.translatable("command.maidquest.no_board"));
            return 0;
        }
        boolean published = QuestApi.publish((ServerLevel) player.level(), boardPos, type,
                quantity, player.getName().getString(), player.level().getGameTime());
        if (!published)
        {
            source.sendFailure(Component.translatable("command.maidquest.publish_failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.maidquest.published",
                type.displayName(), quantity), true);
        return 1;
    }

    private static int list(CommandSourceStack source)
    {
        ServerPlayer player = source.getPlayer();
        if (player == null)
        {
            source.sendFailure(Component.translatable("command.maidquest.player_only"));
            return 0;
        }
        BlockPos boardPos = nearestBoard(player);
        if (boardPos == null)
        {
            source.sendFailure(Component.translatable("command.maidquest.no_board"));
            return 0;
        }
        List<Quest> quests = QuestApi.questsAt((ServerLevel) player.level(), boardPos);
        if (quests.isEmpty())
        {
            source.sendSuccess(() -> Component.translatable("command.maidquest.list_empty"), false);
            return 1;
        }
        for (Quest quest : quests)
        {
            if (quest.status() == QuestStatus.CANCELLED)
            {
                continue;
            }
            IQuestType type = QuestTypeRegistry.get(quest.typeId());
            Component typeName = type == null ? Component.literal(quest.typeId().toString()) : type.displayName();
            source.sendSuccess(() -> Component.translatable("command.maidquest.list_entry",
                    typeName, quest.quantity(), quest.status().getDisplayName(), quest.postedBy()), false);
        }
        return 1;
    }

    private static int boards(CommandSourceStack source)
    {
        ServerPlayer player = source.getPlayer();
        if (player == null)
        {
            source.sendFailure(Component.translatable("command.maidquest.player_only"));
            return 0;
        }
        List<BlockPos> boards = QuestApi.boardsNear((ServerLevel) player.level(),
                player.blockPosition(), BOARD_COMMAND_RADIUS);
        source.sendSuccess(() -> Component.translatable("command.maidquest.boards_count", boards.size()), false);
        for (BlockPos pos : boards)
        {
            source.sendSuccess(() -> Component.literal(pos.toShortString()), false);
        }
        return 1;
    }

    // 支持完整 ID（maidquest:logging）或仅路径名（logging）
    private static IQuestType resolveType(String name)
    {
        Identifier parsed = Identifier.tryParse(name);
        if (parsed != null)
        {
            IQuestType type = QuestTypeRegistry.get(parsed);
            if (type != null && type.isEnabled())
            {
                return type;
            }
        }
        for (IQuestType type : QuestTypeRegistry.getEnabled())
        {
            if (type.id().getPath().equals(name))
            {
                return type;
            }
        }
        return null;
    }

    private static BlockPos nearestBoard(ServerPlayer player)
    {
        List<BlockPos> boards = QuestApi.boardsNear((ServerLevel) player.level(),
                player.blockPosition(), BOARD_COMMAND_RADIUS);
        return boards.isEmpty() ? null : boards.get(0);
    }
}
