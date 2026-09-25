package com.fennecmomo.maidquest.api.event;

import com.fennecmomo.maidquest.api.IQuestType;
import com.fennecmomo.maidquest.api.QuestTypeRegistry;
import net.neoforged.bus.api.Event;

// 委托类型注册事件。
//
// MaidQuest 在世界加载前于 NeoForge 事件总线发出，第三方模组在此注册自定义委托类型：
//
// NeoForge.EVENT_BUS.addListener(RegisterQuestTypesEvent.class, event -> event.register(myType));
public class RegisterQuestTypesEvent extends Event
{
    public void register(IQuestType type)
    {
        QuestTypeRegistry.register(type);
    }
}
