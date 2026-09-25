package com.fennecmomo.maidquest.api;

import com.fennecmomo.maidquest.quest.Quest;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// 委托匹配度加权注册表。
public final class QuestWeightRegistry
{
    private static final Logger LOG = LoggerFactory.getLogger("maidquest:weight");
    private static final int BASE_WEIGHT = 1;
    private static final List<IQuestWeightProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private QuestWeightRegistry()
    {
    }

    public static void register(IQuestWeightProvider provider)
    {
        PROVIDERS.add(provider);
    }

    public static void unregister(IQuestWeightProvider provider)
    {
        PROVIDERS.remove(provider);
    }

    // 汇总权重。单个 provider 抛错不影响其他 provider。
    public static int totalWeight(EntityMaid maid, Quest quest)
    {
        int weight = BASE_WEIGHT;
        for (IQuestWeightProvider provider : PROVIDERS)
        {
            try
            {
                weight += provider.weight(maid, quest);
            }
            catch (RuntimeException e)
            {
                LOG.warn("Quest weight provider {} failed", provider.getClass().getName(), e);
            }
        }
        return Math.max(weight, 0);
    }

    public static void clear()
    {
        PROVIDERS.clear();
    }
}
