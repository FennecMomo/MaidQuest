package com.fennecmomo.maidquest.api;

import com.fennecmomo.maidquest.quest.Quest;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

// 委托匹配度加权接口。
//
// 女仆挑任务时，QuestWeightRegistry 会依次询问所有已注册的 provider，
// 把返回值累加到基础权重上（返回值可为负，最低 0）。
//
// 默认已注册 WorkTypeWeightProvider（按女仆当前 TLM 任务名匹配）。
// 后续模组可注册自己的 provider 叠加技能等级等影响，无需修改 maidquest。
public interface IQuestWeightProvider
{
    int weight(EntityMaid maid, Quest quest);
}
