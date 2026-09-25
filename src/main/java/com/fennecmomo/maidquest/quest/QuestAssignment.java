package com.fennecmomo.maidquest.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// 女仆认领的委托 + 进度。
//
// progress 语义：
//   交付型 → 已存入任务板仓库的数量
//   计数型 → 已完成的工作量
public record QuestAssignment(Quest quest, int progress)
{
    public static final Codec<QuestAssignment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Quest.CODEC.fieldOf("quest").forGetter(QuestAssignment::quest),
            Codec.INT.optionalFieldOf("progress", 0).forGetter(QuestAssignment::progress)
    ).apply(instance, QuestAssignment::new));

    public QuestAssignment withQuest(Quest quest)
    {
        return new QuestAssignment(quest, progress);
    }

    public QuestAssignment withProgress(int progress)
    {
        return new QuestAssignment(quest, progress);
    }
}
