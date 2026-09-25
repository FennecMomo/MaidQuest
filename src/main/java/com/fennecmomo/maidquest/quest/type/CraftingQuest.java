package com.fennecmomo.maidquest.quest.type;

import com.fennecmomo.maidquest.api.IQuestExecutor;
import com.fennecmomo.maidquest.api.IQuestType;
import com.fennecmomo.maidquest.api.RegQuestType;
import com.fennecmomo.maidquest.quest.Quest;
import com.fennecmomo.maidquest.quest.QuestCore;
import com.fennecmomo.maidquest.quest.QuestStatus;
import com.fennecmomo.maidquest.service.QuestInventoryHelper;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;
import java.util.UUID;

// 制造委托（交付型）：带 N 个指定物品到任务板。
//
// 暂未开放（isEnabled = false）：制造执行器未实现，先保留类型占位与 Codec。
public class CraftingQuest extends Quest
{
    private final Identifier item;

    public static final MapCodec<CraftingQuest> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            QuestCore.MAP_CODEC.forGetter(Quest::core),
            Identifier.CODEC.fieldOf("item").forGetter(CraftingQuest::item)
    ).apply(instance, CraftingQuest::new));

    public static final Codec<CraftingQuest> CODEC = MAP_CODEC.codec();

    public CraftingQuest(QuestCore core, Identifier item)
    {
        super(core);
        this.item = item;
    }

    public Identifier item()
    {
        return item;
    }

    @Override
    public Identifier typeId()
    {
        return BuiltinQuestTypes.CRAFTING;
    }

    @Override
    protected Quest withCore(QuestCore core)
    {
        return new CraftingQuest(core, item);
    }

    // ======== 类型定义 ========

    @RegQuestType
    public static class Type implements IQuestType
    {
        @Override
        public Identifier id()
        {
            return BuiltinQuestTypes.CRAFTING;
        }

        @Override
        public Component displayName()
        {
            return Component.translatable("quest.maidquest.crafting");
        }

        @Override
        public ItemStack icon()
        {
            return Items.CRAFTING_TABLE.getDefaultInstance();
        }

        @Override
        public boolean requiresDelivery()
        {
            return true;
        }

        @Override
        public boolean isEnabled()
        {
            return false;
        }

        @Override
        public MapCodec<? extends Quest> codec()
        {
            return MAP_CODEC;
        }

        @Override
        public Quest create(UUID id, String postedBy, int quantity, BlockPos boardPos, long gameTime)
        {
            QuestCore core = new QuestCore(id, postedBy, quantity, QuestStatus.OPEN,
                    Optional.empty(), Optional.empty(), gameTime, 0L, boardPos, Optional.empty());
            return new CraftingQuest(core, BuiltInRegistries.ITEM.getKey(Items.CRAFTING_TABLE));
        }

        @Override
        public IQuestExecutor createExecutor(Quest quest, EntityMaid maid, ServerLevel level)
        {
            return new NoopExecutor();
        }

        @Override
        public int countCarried(Quest quest, EntityMaid maid)
        {
            Identifier wanted = ((CraftingQuest) quest).item();
            return QuestInventoryHelper.count(maid, item -> BuiltInRegistries.ITEM.getKey(item).equals(wanted));
        }

        @Override
        public int deposit(Quest quest, EntityMaid maid, Container target, int maxAmount)
        {
            Identifier wanted = ((CraftingQuest) quest).item();
            return QuestInventoryHelper.deposit(maid, target,
                    item -> BuiltInRegistries.ITEM.getKey(item).equals(wanted), maxAmount);
        }
    }

    // 未开放类型的空执行器
    private static class NoopExecutor implements IQuestExecutor
    {
        @Override
        public boolean tick(EntityMaid maid, ServerLevel level)
        {
            return false;
        }

        @Override
        public boolean isDone()
        {
            return true;
        }

        @Override
        public void stop(EntityMaid maid, ServerLevel level)
        {
        }
    }
}
