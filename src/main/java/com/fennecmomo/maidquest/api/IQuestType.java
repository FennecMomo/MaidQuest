package com.fennecmomo.maidquest.api;

import com.fennecmomo.maidquest.quest.Quest;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// 委托类型接口。
//
// 注册后即可被任务板发布、女仆接取。
// 注册方式：QuestTypeRegistry.register(...)，或 @RegQuestType + QuestTypeRegistry.discover(...)。
public interface IQuestType
{
    // ======== 元数据 ========

    Identifier id();

    Component displayName();

    ItemStack icon();

    // 交付型：产物要送到任务板仓库；计数型：完成计数即可（产物归女仆）。
    boolean requiresDelivery();

    // 是否在任务板界面可选（未实现的类型返回 false，占位用）。
    default boolean isEnabled()
    {
        return true;
    }

    // 与女仆当前工作（TLM 任务 UID）的匹配标识，用于默认匹配度加权；无则 null。
    @Nullable
    default Identifier preferredTaskUid()
    {
        return null;
    }

    // ======== 序列化 ========

    // 类型自身的 MapCodec（公共字段请复用 QuestCore.MAP_CODEC）。
    MapCodec<? extends Quest> codec();

    // ======== 构造 ========

    // 由任务板/API 按默认参数创建委托。
    Quest create(UUID id, String postedBy, int quantity, BlockPos boardPos, long gameTime);

    // ======== 执行 ========

    IQuestExecutor createExecutor(Quest quest, EntityMaid maid, ServerLevel level);

    // ======== 交付（requiresDelivery 为 true 时生效） ========

    // 统计女仆背包中可交付的数量。
    default int countCarried(Quest quest, EntityMaid maid)
    {
        return 0;
    }

    // 最多移入 maxAmount 个可交付物，返回实际移入数量。
    default int deposit(Quest quest, EntityMaid maid, Container target, int maxAmount)
    {
        return 0;
    }
}
