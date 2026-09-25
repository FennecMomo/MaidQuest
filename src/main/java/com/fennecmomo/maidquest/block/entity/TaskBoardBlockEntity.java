package com.fennecmomo.maidquest.block.entity;

import com.fennecmomo.maidquest.quest.Quest;
import com.fennecmomo.maidquest.registry.ModBlockEntities;
import com.fennecmomo.maidquest.service.QuestBoardRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// 任务板方块实体。
//
// 持有两块数据：
//   委托列表（持久化 + 全量同步给客户端渲染）
//   交付仓库 54 格（持久化，客户端通过菜单同步）
public class TaskBoardBlockEntity extends BlockEntity
{
    public static final int STORAGE_COLS = 9;
    public static final int STORAGE_ROWS = 6;
    public static final int STORAGE_SIZE = STORAGE_COLS * STORAGE_ROWS;

    private final List<Quest> quests = new ArrayList<>();

    private final SimpleContainer storage = new SimpleContainer(STORAGE_SIZE)
    {
        @Override
        public void setChanged()
        {
            super.setChanged();
            TaskBoardBlockEntity.this.setChanged();
        }
    };

    public TaskBoardBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.TASK_BOARD.get(), pos, state);
    }

    // ======== 委托 ========

    public List<Quest> getQuests()
    {
        return quests;
    }

    public void addQuest(Quest quest)
    {
        quests.add(quest);
        markUpdated();
    }

    @Nullable
    public Quest findQuest(UUID questId)
    {
        for (Quest quest : quests)
        {
            if (quest.id().equals(questId))
            {
                return quest;
            }
        }
        return null;
    }

    // 按 ID 替换委托（状态流转后回写）
    public void updateQuest(Quest updated)
    {
        for (int i = 0; i < quests.size(); i++)
        {
            if (quests.get(i).id().equals(updated.id()))
            {
                quests.set(i, updated);
                markUpdated();
                return;
            }
        }
    }

    // ======== 交付仓库 ========

    public SimpleContainer getStorage()
    {
        return storage;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state)
    {
        if (level != null)
        {
            Containers.dropContents(level, pos, storage);
        }
        super.preRemoveSideEffects(pos, state);
    }

    // ======== 生命周期 ========

    @Override
    public void setLevel(Level level)
    {
        super.setLevel(level);
        if (level instanceof ServerLevel serverLevel)
        {
            QuestBoardRegistry.register(serverLevel, worldPosition);
        }
    }

    @Override
    public void setRemoved()
    {
        if (level instanceof ServerLevel serverLevel)
        {
            QuestBoardRegistry.unregister(serverLevel, worldPosition);
        }
        super.setRemoved();
    }

    private void markUpdated()
    {
        setChanged();
        if (level != null)
        {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    // ======== 存档 ========

    @Override
    protected void saveAdditional(ValueOutput output)
    {
        super.saveAdditional(output);
        output.store("Quests", Quest.CODEC.listOf(), List.copyOf(quests));
        List<ItemStack> items = new ArrayList<>(STORAGE_SIZE);
        for (int i = 0; i < STORAGE_SIZE; i++)
        {
            items.add(storage.getItem(i));
        }
        output.store("Items", ItemStack.OPTIONAL_CODEC.listOf(), items);
    }

    @Override
    protected void loadAdditional(ValueInput input)
    {
        super.loadAdditional(input);
        quests.clear();
        input.read("Quests", Quest.CODEC.listOf()).ifPresent(quests::addAll);
        input.read("Items", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(items ->
        {
            for (int i = 0; i < Math.min(items.size(), STORAGE_SIZE); i++)
            {
                storage.setItem(i, items.get(i));
            }
        });
    }

    // ======== 客户端同步 ========

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries)
    {
        CompoundTag tag = new CompoundTag();
        tag.store("Quests", Quest.CODEC.listOf(), List.copyOf(quests));
        return tag;
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
