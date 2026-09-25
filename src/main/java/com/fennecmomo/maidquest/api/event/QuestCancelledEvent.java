package com.fennecmomo.maidquest.api.event;

import com.fennecmomo.maidquest.quest.Quest;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

// 委托取消事件（发布者撤销或被放弃时触发）。
public class QuestCancelledEvent extends Event
{
    private final ServerLevel level;
    private final Quest quest;
    private final String cancelledBy;

    public QuestCancelledEvent(ServerLevel level, Quest quest, String cancelledBy)
    {
        this.level = level;
        this.quest = quest;
        this.cancelledBy = cancelledBy;
    }

    public ServerLevel getLevel()
    {
        return level;
    }

    public Quest getQuest()
    {
        return quest;
    }

    public String getCancelledBy()
    {
        return cancelledBy;
    }
}
