package com.fennecmomo.maidquest.api;

import com.fennecmomo.maidquest.MaidQuestConfig;
import com.fennecmomo.maidquest.quest.Quest;
import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.resources.Identifier;

// 默认匹配度加权：女仆当前 TLM 任务名与委托类型名一致时加成。
//
// 例如女仆在做 "maidmorework:logging"（伐木）时，接 "maidquest:logging" 委托的概率更高。
// 后续模组可注册自己的 IQuestWeightProvider 叠加技能等级等影响。
public class WorkTypeWeightProvider implements IQuestWeightProvider
{
    @Override
    public int weight(EntityMaid maid, Quest quest)
    {
        IMaidTask task = maid.getTaskManager().getTask();
        if (task == null)
        {
            return 0;
        }
        Identifier taskUid = task.getUid();
        return taskUid.getPath().equals(quest.typeId().getPath()) ? MaidQuestConfig.WORK_TYPE_MATCH_BONUS : 0;
    }
}
