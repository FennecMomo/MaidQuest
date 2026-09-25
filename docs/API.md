# MaidQuest 公开 API 参考

> 本文档仅列出允许外部模块调用的 API，内部实现类不在此列。
> 最后更新：2026-09-23

---

## 一、对外入口

### QuestApi — 发布/查询/取消

```java
import com.fennecmomo.maidquest.api.QuestApi;
```

| 方法 | 含义 |
|---|---|
| `publish(ServerLevel, BlockPos boardPos, IQuestType, int quantity, String postedBy, long gameTime)` | 按类型默认参数发布委托 |
| `publish(ServerLevel, BlockPos boardPos, Quest)` | 发布已构造好的委托 |
| `questsAt(ServerLevel, BlockPos boardPos)` | 任务板上的委托列表 |
| `currentQuest(EntityMaid)` | 女仆当前认领的委托 |
| `boardsNear(ServerLevel, BlockPos, double radius)` | 范围内任务板位置（按距离排序） |
| `cancel(ServerLevel, BlockPos boardPos, UUID questId, String requester)` | 撤销委托（仅发布者本人） |

### 委托生命周期（服务端自动流转）

```
玩家发布 → 女仆认领 → 执行 → 交付/结算 → 完成
```

女仆侧由 maidquest 行为自动处理，无需外部调用。

---

## 二、扩展委托类型

### IQuestType — 委托类型接口

```java
import com.fennecmomo.maidquest.api.IQuestType;
```

```java
Identifier id();
Component displayName();
ItemStack icon();
boolean requiresDelivery();                  // true=产物入任务板仓库；false=计数完成
boolean isEnabled();                         // 默认 true，未实现类型可返回 false
@Nullable Identifier preferredTaskUid();     // 与女仆当前 TLM 任务匹配用，默认 null
MapCodec<? extends Quest> codec();           // 公共字段复用 QuestCore.MAP_CODEC
Quest create(UUID id, String postedBy, int quantity, BlockPos boardPos, long gameTime);
IQuestExecutor createExecutor(Quest quest, EntityMaid maid, ServerLevel level);
int countCarried(Quest quest, EntityMaid maid);            // 交付型
int deposit(Quest quest, EntityMaid maid, Container target, int maxAmount); // 交付型，最多转移 maxAmount
```

### IQuestExecutor — 执行器接口

```java
import com.fennecmomo.maidquest.api.IQuestExecutor;
```

```java
boolean tick(EntityMaid maid, ServerLevel level);  // 返回 false 结束执行阶段
boolean isDone();
default boolean isBlocked();                       // 因外部约束无法继续（如背包塞不下）→ 行为方暂停而非空转
void stop(EntityMaid maid, ServerLevel level);
```

执行器运行时状态不持久化；进度通过 `QuestService.addProgress(maid, n)` 写入 Attachment。

### 注册方式

```java
// 方式一：直接注册
QuestTypeRegistry.register(myQuestType);

// 方式二：事件（世界加载前，所有模组构造完成后触发）
NeoForge.EVENT_BUS.addListener(RegisterQuestTypesEvent.class, event -> event.register(myQuestType));

// 方式三：@RegQuestType + 扫描自有包（开发环境文件系统有效）
QuestTypeRegistry.discover("your.mod.quest.type");
```

---

## 三、匹配度加权

```java
import com.fennecmomo.maidquest.api.IQuestWeightProvider;
import com.fennecmomo.maidquest.api.QuestWeightRegistry;

public interface IQuestWeightProvider
{
    int weight(EntityMaid maid, Quest quest);   // 附加权重，可负；最终权重最低 0
}

QuestWeightRegistry.register(provider);         // 可叠加多个
```

- 最终权重 = 基础权重 1 + 所有 provider 之和。
- 默认已注册 `WorkTypeWeightProvider`：女仆当前 TLM 任务 UID 的路径名与委托类型路径名一致时加成
  （例如女仆在做 `maidmorework:logging`，接 `maidquest:logging` 委托概率更高）。
- 后续模块可注册 provider 把技能等级等因素叠加进来，无需修改 maidquest。

---

## 四、事件

```java
import com.fennecmomo.maidquest.api.event.*;
```

| 事件 | 触发时机 | 可用数据 |
|---|---|---|
| `QuestPublishedEvent` | 委托加入任务板 | level, quest |
| `QuestClaimedEvent` | 女仆认领 | level, quest, maid |
| `QuestCompletedEvent` | 结算完成 | level, quest, maid |
| `QuestCancelledEvent` | 发布者撤销 | level, quest, cancelledBy |
| `RegisterQuestTypesEvent` | 世界加载前 | 调用 `register(type)` |

---

## 五、命令

| 命令 | 说明 |
|---|---|
| `/maidquest publish <type> <quantity>` | 在最近的任务板发布委托（type 可用路径名，如 logging） |
| `/maidquest list` | 查看最近任务板上的委托 |
| `/maidquest boards` | 查看附近已登记的任务板（调试） |

---

## 六、常量

### MaidQuestConfig

| 常量 | 默认值 | 含义 |
|---|---|---|
| CLAIM_WEIGHT | 30 | 接单行为权重 |
| EXECUTE_WEIGHT | 60 | 执行行为权重 |
| SUBMIT_WEIGHT | 90 | 提交行为权重 |
| WORK_TYPE_MATCH_BONUS | 100 | 工作类型匹配加权 |
| QUEST_BEHAVIOR_PRIORITY | 5 | 注入 WORK 活动的行为优先级 |
| EXECUTOR_MAX_DURATION | 12000 | 行为最长持续 tick |
| BOARD_SEARCH_RADIUS | 32 | 女仆寻找任务板半径 |
| SEARCH_RETRY_TICKS | 100 | 搜索失败重试间隔 |
| TREE_SEARCH_HALF_XZ / TREE_SEARCH_Y_DOWN / TREE_SEARCH_Y_UP | 15 / 1 / 14 | 伐木搜索范围 |
| CHOP_REACH / CHOP_INTERVAL_TICKS | 3 / 20 | 伐木参数 |

> 挖矿等兼容型委托由任务定义方注册（例如 MMW 矿井侧提供 `IQuestType`），maidquest 不内置。
