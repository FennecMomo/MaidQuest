package com.fennecmomo.maidquest.ai;

import com.fennecmomo.fenneclib.behavior.WeightedChildBehavior;
import com.fennecmomo.fenneclib.behavior.WeightedPicker;
import com.fennecmomo.maidquest.MaidQuestConfig;
import com.github.tartaricacid.touhoulittlemaid.api.entity.ai.IExtraMaidBrain;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;

import java.util.ArrayList;
import java.util.List;

// 委托 Brain 扩展。
//
// 通过 TLM 的 IExtraMaidBrain.getWorkBehaviors() 注入所有女仆的 WORK 活动：
// 女仆移动范围内有任务板且板上有可接委托时，默认接单执行，无需手动指派任务。
public class QuestExtraMaidBrain implements IExtraMaidBrain
{
    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getWorkBehaviors()
    {
        WeightedPicker picker = new WeightedPicker(new ArrayList<WeightedChildBehavior>());
        picker.register(new QuestSubmitBehavior());
        picker.register(new QuestExecuteBehavior());
        picker.register(new QuestClaimBehavior());
        return List.of(Pair.of(MaidQuestConfig.QUEST_BEHAVIOR_PRIORITY, picker));
    }
}
