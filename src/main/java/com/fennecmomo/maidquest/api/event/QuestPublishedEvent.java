package com.fennecmomo.maidquest.api.event;

import com.fennecmomo.maidquest.quest.Quest;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

// 委托发布事件（任务板上出现新委托时触发）。
public class QuestPublishedEvent extends Event
{
    private final ServerLevel level;
    private final Quest quest;

    public QuestPublishedEvent(ServerLevel level, Quest quest)
    {
        this.level = level;
        this.quest = quest;
    }

    public ServerLevel getLevel()
    {
        return level;
    }

    public Quest getQuest()
    {
        return quest;
    }
}
