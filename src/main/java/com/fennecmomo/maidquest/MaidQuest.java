package com.fennecmomo.maidquest;

import com.fennecmomo.maidquest.api.QuestTypeRegistry;
import com.fennecmomo.maidquest.api.QuestWeightRegistry;
import com.fennecmomo.maidquest.api.WorkTypeWeightProvider;
import com.fennecmomo.maidquest.api.event.RegisterQuestTypesEvent;
import com.fennecmomo.maidquest.command.QuestCommand;
import com.fennecmomo.maidquest.client.TaskBoardStorageScreen;
import com.fennecmomo.maidquest.network.QuestCancelPayload;
import com.fennecmomo.maidquest.network.QuestOpenStoragePayload;
import com.fennecmomo.maidquest.network.QuestPublishPayload;
import com.fennecmomo.maidquest.quest.type.BuiltinQuestTypes;
import com.fennecmomo.maidquest.registry.ModBlockEntities;
import com.fennecmomo.maidquest.registry.ModBlocks;
import com.fennecmomo.maidquest.registry.ModCreativeTabs;
import com.fennecmomo.maidquest.registry.ModItems;
import com.fennecmomo.maidquest.registry.ModMenus;
import com.fennecmomo.maidquest.service.QuestService;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

// 女仆委托模组入口。
//
// 自注册方块/物品/方块实体/菜单/创造标签/Attachment/网络包/命令，
// 并通过 MaidQuestExtension 向 TLM 注入全女仆通用的接单行为。
@Mod(MaidQuest.MODID)
public class MaidQuest
{
    public static final String MODID = "maidquest";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MaidQuest(IEventBus modEventBus, ModContainer modContainer)
    {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // 内置委托类型与默认加权（世界加载前完成）
        modEventBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(() ->
        {
            BuiltinQuestTypes.registerAll();
            QuestWeightRegistry.register(new WorkTypeWeightProvider());
            NeoForge.EVENT_BUS.post(new RegisterQuestTypesEvent());
            LOGGER.info("MaidQuest registered {} quest types", QuestTypeRegistry.size());
        }));

        // 网络包
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event ->
        {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.playToServer(QuestPublishPayload.TYPE, QuestPublishPayload.STREAM_CODEC,
                    QuestPublishPayload::handle);
            registrar.playToServer(QuestCancelPayload.TYPE, QuestCancelPayload.STREAM_CODEC,
                    QuestCancelPayload::handle);
            registrar.playToServer(QuestOpenStoragePayload.TYPE, QuestOpenStoragePayload.STREAM_CODEC,
                    QuestOpenStoragePayload::handle);
        });

        // 命令
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class,
                event -> QuestCommand.register(event.getDispatcher()));

        // 服务器停止：清理运行时执行器
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class,
                event -> QuestService.clearAll());

        // 女仆死亡：委托退回任务板
        NeoForge.EVENT_BUS.addListener(LivingDeathEvent.class, event ->
        {
            if (event.getEntity() instanceof EntityMaid maid && maid.level() instanceof ServerLevel level)
            {
                QuestService.abandon(level, maid, true);
            }
        });

        // 客户端界面
        if (FMLEnvironment.getDist() == Dist.CLIENT)
        {
            modEventBus.addListener(RegisterMenuScreensEvent.class, event ->
                    event.register(ModMenus.TASK_BOARD.get(), TaskBoardStorageScreen::new));
        }
    }
}
