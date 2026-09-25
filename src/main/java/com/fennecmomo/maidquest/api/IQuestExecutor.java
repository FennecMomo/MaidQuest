package com.fennecmomo.maidquest.api;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;

// 委托执行器。
//
// 每个女仆每份委托一个实例，承载执行阶段的运行时状态（阶段机、目标列表等）。
// 运行时状态不持久化：女仆卸载/重载后由委托类型重建，进度从 Attachment 读取。
public interface IQuestExecutor
{
    // 每 tick 驱动。返回 false 表示执行阶段结束（已达成或无法继续）。
    boolean tick(EntityMaid maid, ServerLevel level);

    // 执行阶段是否已达成（交付型 = 已凑齐交付物；计数型 = 进度达标）。
    boolean isDone();

    // 是否因外部约束无法继续（如背包塞不下产物），由行为方决定暂停而非空转。
    default boolean isBlocked()
    {
        return false;
    }

    // 中断清理（委托被取消/女仆放弃/换委托）。
    void stop(EntityMaid maid, ServerLevel level);
}
