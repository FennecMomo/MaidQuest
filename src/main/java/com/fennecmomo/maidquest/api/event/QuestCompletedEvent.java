package com.fennecmomo.maidquest.api.event;

import com.fennecmomo.maidquest.quest.Quest;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

// 委托完成事件（女仆交付/结算完毕时触发）。
public class QuestCompletedEvent extends Event
{
    private final ServerLevel level;
    private final Quest quest;
    private final EntityMaid maid;

    public QuestCompletedEvent(ServerLevel level, Quest quest, EntityMaid maid)
    {
        this.level = level;
        this.quest = quest;
        this.maid = maid;
    }

    public ServerLevel getLevel()
    {
        return level;
    }

    public Quest getQuest()
    {
        return quest;
    }

    public EntityMaid getMaid()
    {
        return maid;
    }
}
