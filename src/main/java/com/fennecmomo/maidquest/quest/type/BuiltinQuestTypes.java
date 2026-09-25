package com.fennecmomo.maidquest.quest.type;

import com.fennecmomo.maidquest.MaidQuest;
import com.fennecmomo.maidquest.api.QuestTypeRegistry;
import net.minecraft.resources.Identifier;

// 内置委托类型 ID 与注册入口。
public final class BuiltinQuestTypes
{
    public static final Identifier LOGGING = Identifier.fromNamespaceAndPath(MaidQuest.MODID, "logging");
    public static final Identifier CRAFTING = Identifier.fromNamespaceAndPath(MaidQuest.MODID, "crafting");
    public static final Identifier BUILDING = Identifier.fromNamespaceAndPath(MaidQuest.MODID, "building");

    private BuiltinQuestTypes()
    {
    }

    // 显式注册内置类型（生产环境同样可靠，不依赖类路径扫描）。
    // 兼容型委托（如矿井挖矿）由任务定义方通过 IQuestType 自行注册。
    public static void registerAll()
    {
        QuestTypeRegistry.register(new LoggingQuest.Type());
        QuestTypeRegistry.register(new CraftingQuest.Type());
        QuestTypeRegistry.register(new BuildingQuest.Type());
    }
}
