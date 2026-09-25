package com.fennecmomo.maidquest;

import com.fennecmomo.maidquest.quest.QuestAssignment;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;

// 女仆委托 Attachment 注册。
//
// 认领的委托与进度随女仆实体持久化，区块卸载/重启不丢。
public class ModAttachments
{
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MaidQuest.MODID);

    // 当前认领的委托（空 = 没接单）
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Optional<QuestAssignment>>> QUEST_ASSIGNMENT =
            ATTACHMENT_TYPES.register("quest_assignment", () ->
                    AttachmentType.<Optional<QuestAssignment>>builder(Optional::empty)
                            .serialize(new IAttachmentSerializer<Optional<QuestAssignment>>()
                            {
                                @Override
                                public Optional<QuestAssignment> read(IAttachmentHolder holder, ValueInput input)
                                {
                                    return input.read("assignment", QuestAssignment.CODEC);
                                }

                                @Override
                                public boolean write(Optional<QuestAssignment> value, ValueOutput output)
                                {
                                    if (value.isEmpty())
                                    {
                                        return false;
                                    }
                                    output.store("assignment", QuestAssignment.CODEC, value.get());
                                    return true;
                                }
                            })
                            .build());
}
