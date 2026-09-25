package com.fennecmomo.maidquest.quest.type;

import com.fennecmomo.maidquest.api.IQuestExecutor;
import com.fennecmomo.maidquest.api.IQuestType;
import com.fennecmomo.maidquest.api.RegQuestType;
import com.fennecmomo.maidquest.quest.Quest;
import com.fennecmomo.maidquest.quest.QuestCore;
import com.fennecmomo.maidquest.quest.QuestStatus;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;
import java.util.UUID;

// 建造委托（计数型）：在目标位置放置 N 个指定方块。
//
// 暂未开放（isEnabled = false）：建造执行器未实现，先保留类型占位与 Codec。
public class BuildingQuest extends Quest
{
    private final Identifier block;

    public static final MapCodec<BuildingQuest> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            QuestCore.MAP_CODEC.forGetter(Quest::core),
            Identifier.CODEC.fieldOf("block").forGetter(BuildingQuest::block)
    ).apply(instance, BuildingQuest::new));

    public static final Codec<BuildingQuest> CODEC = MAP_CODEC.codec();

    public BuildingQuest(QuestCore core, Identifier block)
    {
        super(core);
        this.block = block;
    }

    public Identifier block()
    {
        return block;
    }

    @Override
    public Identifier typeId()
    {
        return BuiltinQuestTypes.BUILDING;
    }

    @Override
    protected Quest withCore(QuestCore core)
    {
        return new BuildingQuest(core, block);
    }

    // ======== 类型定义 ========

    @RegQuestType
    public static class Type implements IQuestType
    {
        @Override
        public Identifier id()
        {
            return BuiltinQuestTypes.BUILDING;
        }

        @Override
        public Component displayName()
        {
            return Component.translatable("quest.maidquest.building");
        }

        @Override
        public ItemStack icon()
        {
            return Items.BRICKS.getDefaultInstance();
        }

        @Override
        public boolean requiresDelivery()
        {
            return false;
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
                    Optional.empty(), Optional.empty(), gameTime, 0L, boardPos, Optional.of(boardPos));
            return new BuildingQuest(core, BuiltInRegistries.BLOCK.getKey(Blocks.BRICKS));
        }

        @Override
        public IQuestExecutor createExecutor(Quest quest, EntityMaid maid, ServerLevel level)
        {
            return new NoopExecutor();
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
