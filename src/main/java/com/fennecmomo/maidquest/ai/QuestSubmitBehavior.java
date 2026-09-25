package com.fennecmomo.maidquest.ai;

import com.fennecmomo.fenneclib.behavior.WeightedChildBehavior;
import com.fennecmomo.maidquest.MaidQuestConfig;
import com.fennecmomo.maidquest.block.entity.TaskBoardBlockEntity;
import com.fennecmomo.maidquest.quest.QuestAssignment;
import com.fennecmomo.maidquest.service.QuestFeedback;
import com.fennecmomo.maidquest.service.QuestService;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.Optional;

// 提交行为。
//
// 满足提交条件（交付型凑齐/背包满；计数型进度达标）时，走回委托所属任务板结算。
// 任务板已被拆除时放弃委托。
public class QuestSubmitBehavior extends WeightedChildBehavior
{
    private enum State { IDLE, MOVING, SUBMITTING }

    private State state = State.IDLE;
    private boolean finished;

    public QuestSubmitBehavior()
    {
        super(Map.of(), MaidQuestConfig.EXECUTOR_MAX_DURATION);
    }

    // ======== 权重与条件 ========

    @Override
    public int getWeight(EntityMaid maid)
    {
        return QuestService.isReadyToSubmit(maid) ? MaidQuestConfig.SUBMIT_WEIGHT : 0;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid)
    {
        return QuestService.isReadyToSubmit(maid);
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
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long time)
    {
        Optional<QuestAssignment> optional = QuestService.assignment(maid);
        if (optional.isEmpty())
        {
            finished = true;
            return;
        }
        BlockPos boardPos = optional.get().quest().boardPos();
        switch (state)
        {
            case IDLE -> state = State.MOVING;
            case MOVING ->
            {
                // 板子所在区块已加载但方块实体不存在 → 被拆除，放弃委托
                if (level.isLoaded(boardPos) && !(level.getBlockEntity(boardPos) instanceof TaskBoardBlockEntity))
                {
                    QuestFeedback.bubble(maid, "bubble.maidquest.board_lost");
                    QuestService.abandon(level, maid, false);
                    finished = true;
                    return;
                }
                if (maid.blockPosition().distSqr(boardPos) <= MaidQuestConfig.ARRIVE_DIST_SQ)
                {
                    state = State.SUBMITTING;
                    return;
                }
                maid.getNavigation().moveTo(boardPos.getX() + 0.5, boardPos.getY(), boardPos.getZ() + 0.5, 0.6);
            }
            case SUBMITTING ->
            {
                maid.getNavigation().stop();
                QuestService.submit(level, maid);
                finished = true;
            }
        }
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long time)
    {
        maid.getNavigation().stop();
        finished = false;
        state = State.IDLE;
    }
}
