package com.fennecmomo.maidquest;

import com.fennecmomo.maidquest.ai.QuestExtraMaidBrain;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.ExtraMaidBrainManager;

// TLM 扩展入口。
//
// 通过 addExtraMaidBrain 注册委托行为，注入所有女仆的 WORK 活动：
// 女仆移动范围内有任务板且板上有可接委托时默认接单，无需手动指派任务。
@LittleMaidExtension
public class MaidQuestExtension implements ILittleMaid
{
    @Override
    public void addExtraMaidBrain(ExtraMaidBrainManager manager)
    {
        manager.addExtraMaidBrain(new QuestExtraMaidBrain());
        MaidQuest.LOGGER.info("MaidQuest quest behaviors injected into maid WORK activity");
    }
}
