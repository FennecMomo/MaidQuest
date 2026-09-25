package com.fennecmomo.maidquest.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

// 委托公共数据——所有委托类型共享的持久化字段。
// 委托本体不可变，状态流转返回新实例。
public record QuestCore(UUID id, String postedBy, int quantity, QuestStatus status,
                        Optional<UUID> claimedBy, Optional<String> claimedByName,
                        long createdAt, long completedAt,
                        BlockPos boardPos, Optional<BlockPos> targetPos)
{
    public static final MapCodec<QuestCore> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(QuestCore::id),
            Codec.STRING.fieldOf("postedBy").forGetter(QuestCore::postedBy),
            Codec.INT.fieldOf("quantity").forGetter(QuestCore::quantity),
            QuestStatus.CODEC.fieldOf("status").forGetter(QuestCore::status),
            UUIDUtil.CODEC.optionalFieldOf("claimedBy").forGetter(QuestCore::claimedBy),
            Codec.STRING.optionalFieldOf("claimedByName").forGetter(QuestCore::claimedByName),
            Codec.LONG.fieldOf("createdAt").forGetter(QuestCore::createdAt),
            Codec.LONG.optionalFieldOf("completedAt", 0L).forGetter(QuestCore::completedAt),
            BlockPos.CODEC.fieldOf("boardPos").forGetter(QuestCore::boardPos),
            BlockPos.CODEC.optionalFieldOf("targetPos").forGetter(QuestCore::targetPos)
    ).apply(instance, QuestCore::new));

    // ======== 状态流转 ========

    public QuestCore withStatus(QuestStatus status)
    {
        return new QuestCore(id, postedBy, quantity, status, claimedBy, claimedByName,
                createdAt, completedAt, boardPos, targetPos);
    }

    public QuestCore claimed(UUID maidUuid, String maidName)
    {
        return new QuestCore(id, postedBy, quantity, QuestStatus.CLAIMED,
                Optional.of(maidUuid), Optional.of(maidName), createdAt, completedAt, boardPos, targetPos);
    }

    public QuestCore started()
    {
        return withStatus(QuestStatus.IN_PROGRESS);
    }

    public QuestCore completed(long time)
    {
        return new QuestCore(id, postedBy, quantity, QuestStatus.COMPLETED, claimedBy, claimedByName,
                createdAt, time, boardPos, targetPos);
    }

    public QuestCore cancelled()
    {
        return withStatus(QuestStatus.CANCELLED);
    }

    // 女仆放弃/任务板失联：退回待接取，清除认领信息
    public QuestCore reopened()
    {
        return new QuestCore(id, postedBy, quantity, QuestStatus.OPEN,
                Optional.empty(), Optional.empty(), createdAt, completedAt, boardPos, targetPos);
    }
}
