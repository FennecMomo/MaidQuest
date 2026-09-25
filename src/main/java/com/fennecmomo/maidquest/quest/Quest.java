package com.fennecmomo.maidquest.quest;

import com.fennecmomo.maidquest.api.IQuestType;
import com.fennecmomo.maidquest.api.QuestTypeRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.UUID;

// 委托基类。
//
// 持久化数据不可变：状态流转（认领/执行/完成/取消）一律返回新实例，
// 由任务板方块实体与女仆 Attachment 分别持有副本，运行时执行状态放 IQuestExecutor。
public abstract class Quest
{
    // 按 typeId 多态分派，类型表见 QuestTypeRegistry
    public static final Codec<Quest> CODEC =
            Identifier.CODEC.dispatch(Quest::typeId, id -> QuestTypeRegistry.getOrThrow(id).codec());

    private final QuestCore core;

    protected Quest(QuestCore core)
    {
        this.core = core;
    }

    // ======== 类型 ========

    public abstract Identifier typeId();

    public IQuestType type()
    {
        return QuestTypeRegistry.getOrThrow(typeId());
    }

    // 子类用新 core 创建副本
    protected abstract Quest withCore(QuestCore core);

    // ======== 状态流转 ========

    public final Quest claimed(UUID maidUuid, String maidName)
    {
        return withCore(core.claimed(maidUuid, maidName));
    }

    public final Quest started()
    {
        return withCore(core.started());
    }

    public final Quest completed(long time)
    {
        return withCore(core.completed(time));
    }

    public final Quest cancelled()
    {
        return withCore(core.cancelled());
    }

    public final Quest reopened()
    {
        return withCore(core.reopened());
    }

    // ======== 数据访问 ========

    public QuestCore core()
    {
        return core;
    }

    public UUID id()
    {
        return core.id();
    }

    public String postedBy()
    {
        return core.postedBy();
    }

    public int quantity()
    {
        return core.quantity();
    }

    public QuestStatus status()
    {
        return core.status();
    }

    public Optional<UUID> claimedBy()
    {
        return core.claimedBy();
    }

    public Optional<String> claimedByName()
    {
        return core.claimedByName();
    }

    public long createdAt()
    {
        return core.createdAt();
    }

    public long completedAt()
    {
        return core.completedAt();
    }

    // 所属任务板位置
    public BlockPos boardPos()
    {
        return core.boardPos();
    }

    // 可选的工作目标位置（挖矿/建造类使用）
    public Optional<BlockPos> targetPos()
    {
        return core.targetPos();
    }
}
