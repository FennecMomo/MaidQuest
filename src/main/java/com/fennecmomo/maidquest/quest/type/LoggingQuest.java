package com.fennecmomo.maidquest.quest.type;

import com.fennecmomo.maidquest.MaidQuestConfig;
import com.fennecmomo.maidquest.api.IQuestExecutor;
import com.fennecmomo.maidquest.api.IQuestType;
import com.fennecmomo.maidquest.api.RegQuestType;
import com.fennecmomo.maidquest.quest.Quest;
import com.fennecmomo.maidquest.quest.QuestCore;
import com.fennecmomo.maidquest.quest.QuestStatus;
import com.fennecmomo.maidquest.service.QuestFeedback;
import com.fennecmomo.maidquest.service.QuestInventoryHelper;
import com.fennecmomo.maidquest.service.QuestService;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// 伐木委托（交付型）：砍够 N 个原木并送到任务板仓库。
public class LoggingQuest extends Quest
{
    public static final MapCodec<LoggingQuest> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            QuestCore.MAP_CODEC.forGetter(Quest::core)
    ).apply(instance, LoggingQuest::new));

    public static final Codec<LoggingQuest> CODEC = MAP_CODEC.codec();

    public LoggingQuest(QuestCore core)
    {
        super(core);
    }

    @Override
    public Identifier typeId()
    {
        return BuiltinQuestTypes.LOGGING;
    }

    @Override
    protected Quest withCore(QuestCore core)
    {
        return new LoggingQuest(core);
    }

    // ======== 类型定义 ========

    @RegQuestType
    public static class Type implements IQuestType
    {
        @Override
        public Identifier id()
        {
            return BuiltinQuestTypes.LOGGING;
        }

        @Override
        public Component displayName()
        {
            return Component.translatable("quest.maidquest.logging");
        }

        @Override
        public ItemStack icon()
        {
            return Items.IRON_AXE.getDefaultInstance();
        }

        @Override
        public boolean requiresDelivery()
        {
            return true;
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
            return new LoggingQuest(core);
        }

        @Override
        public IQuestExecutor createExecutor(Quest quest, EntityMaid maid, ServerLevel level)
        {
            return new Executor((LoggingQuest) quest);
        }

        @Override
        public int countCarried(Quest quest, EntityMaid maid)
        {
            return QuestInventoryHelper.count(maid, LoggingQuest::isLog);
        }

        @Override
        public int deposit(Quest quest, EntityMaid maid, Container target, int maxAmount)
        {
            return QuestInventoryHelper.deposit(maid, target, LoggingQuest::isLog, maxAmount);
        }
    }

    // ======== 物品判定 ========

    public static boolean isLog(Item item)
    {
        return item instanceof BlockItem blockItem
                && blockItem.getBlock().defaultBlockState().is(BlockTags.LOGS);
    }

    // ======== 执行器 ========

    public static class Executor implements IQuestExecutor
    {
        private enum Phase { SEARCHING, WALKING, CHOPPING }

        // 连续搜索失败多少次后弹一次气泡（约 15 秒）
        private static final int BUBBLE_FAILURE_INTERVAL = 3;

        private final LoggingQuest quest;
        private final List<BlockPos> logs = new ArrayList<>();
        private Phase phase = Phase.SEARCHING;
        private BlockPos moveTarget;
        private int tickInPhase;
        private int searchCooldown;
        private int searchFailures;
        private boolean done;
        private boolean blocked;

        public Executor(LoggingQuest quest)
        {
            this.quest = quest;
        }

        @Override
        public boolean tick(EntityMaid maid, ServerLevel level)
        {
            if (checkDone(maid))
            {
                return false;
            }
            tickInPhase++;
            switch (phase)
            {
                case SEARCHING -> tickSearch(maid, level);
                case WALKING -> tickWalk(maid, level);
                case CHOPPING -> tickChop(maid, level);
            }
            return !checkDone(maid);
        }

        @Override
        public boolean isDone()
        {
            return done;
        }

        @Override
        public boolean isBlocked()
        {
            return blocked;
        }

        @Override
        public void stop(EntityMaid maid, ServerLevel level)
        {
            maid.getNavigation().stop();
        }

        // 凑齐（已入库 + 身上携带）或掉落塞不下（背包满且手上没有可交付物）→ 结束执行阶段
        private boolean checkDone(EntityMaid maid)
        {
            int delivered = QuestService.progress(maid);
            int carried = quest.type().countCarried(quest, maid);
            if (delivered + carried >= quest.quantity())
            {
                done = true;
                return true;
            }
            if (carried == 0 && QuestService.isInventoryFull(maid))
            {
                done = true;
                blocked = true;
                return true;
            }
            return false;
        }

        // ======== Phase: SEARCHING ========

        private void tickSearch(EntityMaid maid, ServerLevel level)
        {
            if (searchCooldown > 0)
            {
                searchCooldown--;
                return;
            }
            if (!scanRegion(level, maid.blockPosition()))
            {
                searchFailures++;
                if (searchFailures % BUBBLE_FAILURE_INTERVAL == 0)
                {
                    QuestFeedback.bubble(maid, "bubble.maidquest.no_tree");
                }
                searchCooldown = MaidQuestConfig.SEARCH_RETRY_TICKS;
                return;
            }
            searchFailures = 0;
            logs.sort(Comparator.comparingDouble(pos -> pos.distSqr(maid.blockPosition())));
            BlockPos lowest = logs.stream().min(Comparator.comparingInt(BlockPos::getY)).orElse(null);
            moveTarget = lowest == null ? null : lowest.below();
            phase = Phase.WALKING;
            tickInPhase = 0;
        }

        // 扫描周边找一棵带树叶的原木作为树脚，命中后 BFS 整棵树
        private boolean scanRegion(ServerLevel level, BlockPos center)
        {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            int halfXz = MaidQuestConfig.TREE_SEARCH_HALF_XZ;
            for (int x = center.getX() - halfXz; x <= center.getX() + halfXz; x++)
            {
                for (int z = center.getZ() - halfXz; z <= center.getZ() + halfXz; z++)
                {
                    for (int y = center.getY() - MaidQuestConfig.TREE_SEARCH_Y_DOWN;
                         y <= center.getY() + MaidQuestConfig.TREE_SEARCH_Y_UP; y++)
                    {
                        cursor.set(x, y, z);
                        if (!level.isLoaded(cursor))
                        {
                            continue;
                        }
                        if (level.getBlockState(cursor).is(BlockTags.LOGS) && hasAdjacentLeaves(level, cursor))
                        {
                            bfsTree(level, cursor.immutable());
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private boolean hasAdjacentLeaves(ServerLevel level, BlockPos log)
        {
            for (Direction direction : Direction.values())
            {
                if (level.getBlockState(log.relative(direction)).is(BlockTags.LEAVES))
                {
                    return true;
                }
            }
            return false;
        }

        // 沿原木+树叶连通域遍历，只收集原木（树叶不砍，让其自然消失）
        private void bfsTree(ServerLevel level, BlockPos start)
        {
            Set<BlockPos> visited = new HashSet<>();
            Deque<BlockPos> queue = new ArrayDeque<>();
            queue.add(start);
            visited.add(start);
            logs.clear();
            while (!queue.isEmpty())
            {
                BlockPos pos = queue.poll();
                BlockState state = level.getBlockState(pos);
                if (state.is(BlockTags.LOGS))
                {
                    logs.add(pos);
                }
                else if (!state.is(BlockTags.LEAVES))
                {
                    continue;
                }
                for (Direction direction : Direction.values())
                {
                    BlockPos next = pos.relative(direction);
                    if (visited.contains(next))
                    {
                        continue;
                    }
                    BlockState nextState = level.getBlockState(next);
                    if (nextState.is(BlockTags.LOGS) || nextState.is(BlockTags.LEAVES))
                    {
                        visited.add(next);
                        queue.add(next);
                    }
                }
            }
        }

        // ======== Phase: WALKING ========

        private void tickWalk(EntityMaid maid, ServerLevel level)
        {
            if (moveTarget == null)
            {
                phase = Phase.CHOPPING;
                return;
            }
            maid.getNavigation().moveTo(moveTarget.getX() + 0.5, moveTarget.getY(), moveTarget.getZ() + 0.5, 0.6);
            if (maid.blockPosition().distSqr(moveTarget) <= MaidQuestConfig.CHOP_REACH * MaidQuestConfig.CHOP_REACH)
            {
                QuestInventoryHelper.equipAxe(maid);
                phase = Phase.CHOPPING;
                tickInPhase = 0;
            }
        }

        // ======== Phase: CHOPPING ========

        private void tickChop(EntityMaid maid, ServerLevel level)
        {
            if (logs.isEmpty())
            {
                phase = Phase.SEARCHING;
                searchCooldown = 0;
                return;
            }
            if (tickInPhase % MaidQuestConfig.CHOP_INTERVAL_TICKS != 0)
            {
                return;
            }
            BlockPos target = logs.get(logs.size() - 1);
            maid.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
            maid.swing(maid.getUsedItemHand());
            level.sendParticles(ParticleTypes.CRIT, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                    2, 0.15, 0.15, 0.15, 0);
            int carriedBefore = quest.type().countCarried(quest, maid);
            if (destroyBlock(maid, level, logs.remove(logs.size() - 1)))
            {
                maid.playSound(SoundEvents.WOOD_BREAK, 1.0f, 1.0f);
                // 产物没进背包（塞不下掉地上）→ 判定受阻，结束执行阶段交给暂停/交付流程
                if (quest.type().countCarried(quest, maid) <= carriedBefore)
                {
                    done = true;
                    blocked = true;
                }
            }
        }

        private boolean destroyBlock(EntityMaid maid, ServerLevel level, BlockPos pos)
        {
            BlockState state = level.getBlockState(pos);
            if (state.isAir())
            {
                return false;
            }
            maid.getItemManager().dropResourcesToMaidInv(state, level, pos, level.getBlockEntity(pos),
                    maid.getMainHandItem());
            level.destroyBlock(pos, false, maid);
            maid.getMainHandItem().hurtAndBreak(1, maid, EquipmentSlot.MAINHAND);
            return true;
        }
    }
}
