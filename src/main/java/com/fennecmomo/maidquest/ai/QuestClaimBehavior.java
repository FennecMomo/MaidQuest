package com.fennecmomo.maidquest.ai;

import com.fennecmomo.fenneclib.behavior.WeightedChildBehavior;
import com.fennecmomo.maidquest.MaidQuestConfig;
import com.fennecmomo.maidquest.quest.Quest;
import com.fennecmomo.maidquest.service.QuestFeedback;
import com.fennecmomo.maidquest.service.QuestMatcher;
import com.fennecmomo.maidquest.service.QuestService;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Map;

// 接单行为。
//
// 没有在执行的委托时，找移动范围内最近的有可接委托的任务板 → 走过去 → 按匹配度加权抽一个 → 认领。
public class QuestClaimBehavior extends WeightedChildBehavior
{
    private enum State { IDLE, MOVING, CLAIMING }

    private State state = State.IDLE;
    private BlockPos targetBoard;
    private boolean finished;

    public QuestClaimBehavior()
    {
        super(Map.of(), MaidQuestConfig.EXECUTOR_MAX_DURATION);
    }

    // ======== 权重与条件 ========

    @Override
    public int getWeight(EntityMaid maid)
    {
        if (QuestService.assignment(maid).isPresent())
        {
            return 0;
        }
        return findBoard(maid) == null ? 0 : MaidQuestConfig.CLAIM_WEIGHT;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid)
    {
        return QuestService.assignment(maid).isEmpty() && findBoard(maid) != null;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long time)
    {
        return !finished;
    }

    // ======== 生命周期 ========

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long time)
    {
        finished = false;
        state = State.IDLE;
        targetBoard = null;
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long time)
    {
        switch (state)
        {
            case IDLE ->
            {
                targetBoard = findBoard(maid);
                if (targetBoard == null)
                {
                    finished = true;
                    return;
                }
                moveToBoard(maid);
                state = State.MOVING;
            }
            case MOVING ->
            {
                if (targetBoard == null)
                {
                    finished = true;
                    return;
                }
                moveToBoard(maid);
                if (maid.blockPosition().distSqr(targetBoard) <= MaidQuestConfig.ARRIVE_DIST_SQ)
                {
                    state = State.CLAIMING;
                }
            }
            case CLAIMING -> claim(level, maid);
        }
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long time)
    {
        maid.getNavigation().stop();
        finished = false;
        state = State.IDLE;
        targetBoard = null;
    }

    // ======== 逻辑 ========

    private BlockPos findBoard(EntityMaid maid)
    {
        if (!(maid.level() instanceof ServerLevel level))
        {
            return null;
        }
        return QuestMatcher.findBoardWithClaimableQuest(level, maid, MaidQuestConfig.BOARD_SEARCH_RADIUS);
    }

    private void moveToBoard(EntityMaid maid)
    {
        maid.getNavigation().moveTo(targetBoard.getX() + 0.5, targetBoard.getY(), targetBoard.getZ() + 0.5, 0.6);
    }

    private void claim(ServerLevel level, EntityMaid maid)
    {
        List<Quest> claimable = QuestMatcher.claimableQuests(level, targetBoard, maid);
        Quest chosen = QuestMatcher.pickWeighted(maid, claimable);
        if (chosen != null && QuestService.claim(level, maid, targetBoard, chosen.id()))
        {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    maid.getX(), maid.getEyeY(), maid.getZ(), 5, 0.3, 0.2, 0.3, 0);
            QuestFeedback.playClaim(maid);
        }
        finished = true;
    }
}
