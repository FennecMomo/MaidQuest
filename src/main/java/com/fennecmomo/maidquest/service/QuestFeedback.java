package com.fennecmomo.maidquest.service;

import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.TextChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;

// 女仆反馈：TLM 女仆音效 + 聊天气泡。
public final class QuestFeedback
{
    private QuestFeedback()
    {
    }

    // 接单
    public static void playClaim(EntityMaid maid)
    {
        play(maid, InitSounds.MAID_TAMED.get());
    }

    // 交付/完成
    public static void playDeliver(EntityMaid maid)
    {
        play(maid, InitSounds.MAID_ITEM_GET.get());
    }

    public static void play(EntityMaid maid, SoundEvent sound)
    {
        maid.playSound(sound, 1.0f, 1.0f);
    }

    // 气泡提示（组件同步，客户端按本地语言渲染）
    public static void bubble(EntityMaid maid, String translationKey)
    {
        maid.getChatBubbleManager().addChatBubble(TextChatBubbleData.type1(Component.translatable(translationKey)));
    }
}
