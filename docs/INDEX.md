# MaidQuest 文档

> 版本：0.0.1（重构完成，待游戏内验证） | MC 26.1.2 / NeoForge | JDK 25
> 全项目待办总览：[../../docs/TODO.md](../../docs/TODO.md)

女仆委托模组：任务板发布委托，普通 TLM 女仆在移动范围内有委托板时默认接单、执行并交付。

## 文档列表

| 文档 | 说明 |
|------|------|
| [API.md](API.md) | 公开 API 参考：QuestApi / IQuestType / 注册表 / 事件 / 匹配度加权 |
| [订单生产链设计](../../docs/design/MAIDQUEST_MMW_ORDER_CHAIN.md) | 与 MaidMoreWork 的委托倾向、工分工资和公共仓库交易设计（讨论中，非当前功能） |
| [TEST_LOGGING.md](TEST_LOGGING.md) | 当前测试清单（操作人用） |
| [CHANGELOG.md](CHANGELOG.md) | 版本更新记录（仅作者明确要求时记录） |

## 包结构

```
com.fennecmomo.maidquest/
  MaidQuest.java                        # @Mod 入口（自注册全部内容）
  MaidQuestExtension.java               # TLM 扩展入口（注入全女仆接单行为）
  MaidQuestConfig.java                  # 可调常量
  ModAttachments.java                   # 认领委托 Attachment
  api/
    QuestApi.java                       # 对外静态入口
    IQuestType.java                     # 委托类型接口
    IQuestExecutor.java                 # 执行器接口
    QuestTypeRegistry.java              # 类型注册表（+ @RegQuestType 发现）
    IQuestWeightProvider.java           # 匹配度加权接口
    QuestWeightRegistry.java            # 加权注册表
    WorkTypeWeightProvider.java         # 默认加权（当前工作类型匹配）
    event/                              # 发布/认领/完成/取消 + 类型注册事件
  quest/
    Quest.java / QuestCore.java / QuestStatus.java / QuestAssignment.java
    type/                               # 内置类型：伐木（交付型）；制造/建造为未开放占位
                                        # 兼容型委托（矿井挖矿等）由任务定义方注册
  service/
    QuestService.java                   # 发布/认领/进度/提交/放弃/暂停恢复
    QuestBoardRegistry.java / QuestBoardData.java   # 任务板登记（SavedData）
    QuestMatcher.java                   # 可接委托筛选与加权抽取
    QuestInventoryHelper.java           # TLM 背包读写工具（与掉落同 handler）
    QuestFeedback.java                  # TLM 女仆音效 + 聊天气泡
  ai/
    QuestExtraMaidBrain.java            # IExtraMaidBrain 注入 WORK 活动
    QuestClaimBehavior.java / QuestExecuteBehavior.java / QuestSubmitBehavior.java
  block/ + menu/ + client/ + network/ + command/ + registry/
```

## 设计要点

- **普通女仆标准**：行为通过 TLM `IExtraMaidBrain.getWorkBehaviors()` 注入所有女仆，无需指派任务；背包读写走 TLM `MaidItemManager`。
- **不依赖 maidmorework/maidtown**：只依赖 TLM + FennecLib。
- **无经济**：委托无偿；交付型把产物存入任务板「交付仓库」，计数型产物归女仆。
- **持久化**：委托本体在任务板方块实体；认领关系与进度在女仆 Attachment；执行器运行时状态可重建。
- **匹配度加权**：默认按女仆当前 TLM 任务名匹配；第三方可注册 `IQuestWeightProvider` 叠加技能等影响。
