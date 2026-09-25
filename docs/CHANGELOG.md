# MaidQuest CHANGELOG

> 仅作者明确要求「更新版本」时记录。当前 0.0.1 为重构开发版，尚未发布。

## 0.0.1（未发布）

### 新增
- 独立模组骨架：@Mod 入口、方块/物品/方块实体/菜单/创造标签/网络包/命令自注册
- 委托框架：QuestApi、IQuestType/IQuestExecutor、类型注册表、发布/认领/完成/取消事件
- 匹配度加权：IQuestWeightProvider + 默认工作类型匹配 provider
- 任务板方块 + 委托界面 + 54 格交付仓库（自绘面板 + 返回按钮）
- 伐木委托（交付型，BFS 整树、只砍原木）
- 普通 TLM 女仆默认接单（IExtraMaidBrain 注入）
- 认领与进度 Attachment 持久化
- 女仆反馈：TLM 女仆音效 + 聊天气泡提示
- 满背包保护：手上有时先卸货，没有时暂停并在腾出空间后自动恢复

### 修改
- 全量重构旧版 task/skill/IMaidquestHost 体系，去除 maidmorework 依赖与静态内存任务表
- 仓库界面改为原版箱子贴图风格；委托列表按签名重建（修按钮残留与 widget 堆积）
- 执行中拆掉委托板 → 放弃委托，不再卡死

### 删除
- 旧任务模型（Task/TaskService/TaskStatus）、旧行为（TaskClaim/TaskExecute/TaskSubmit）
- Skill 枚举（技能加权改为 provider 扩展点）
- 内置挖矿委托（兼容型委托改由任务定义方通过 IQuestType 注册）
