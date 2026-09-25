package com.fennecmomo.maidquest.quest;

import com.mojang.serialization.Codec;

// 委托生命周期。
//
// OPEN → CLAIMED → IN_PROGRESS → COMPLETED
// (任意阶段 → CANCELLED)
public enum QuestStatus
{
    OPEN("待接取"),
    CLAIMED("已认领"),
    IN_PROGRESS("执行中"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    public static final Codec<QuestStatus> CODEC = Codec.STRING.xmap(QuestStatus::valueOf, QuestStatus::name);

    private final String displayName;

    QuestStatus(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }
}
