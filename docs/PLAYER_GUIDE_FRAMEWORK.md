# 以太工艺 (Ether Craft) — 玩家指南框架

> 本文档是指南的框架与内容大纲，包含每个章节的详细内容描述、对应的代码位置、以及关键代码片段。
> 最终指南将基于此框架，按照 docs/misc/mc_guide.md 的写作规范进行润色。

---

## 目录总览

| 章节 | 标题 | 阶段 | 类型 |
|------|------|------|------|
| 1 | 欢迎来到以太工艺 | E1 | 概念解释 + 入门 |
| 2 | 以太适应节点 — 核心机器 | E1-E2 | 物品说明书 + 操作向导 |
| 3 | 以太加工中心 — 自动化加工 | E1-E4 | 物品说明书 + 操作向导 |
| 4 | 以太流系统 — 能量与物流 | E2 | 概念解释 + 操作向导 |
| 5 | 镀层系统 — 装备强化 | E3 | 概念解释 + 物品说明书 |
| 6 | 终局之路 — 三大分支 | E4 | 概念解释 |

---

## 第一章：欢迎来到以太工艺

### 1.1 以太是什么？（1 页）

**内容**：
- 以太是构成万物的基本元素，古典定义，近代才被发现和利用
- 你是第一批探索者，没有远古文明遗产
- 视觉风格：机器外观科技感（金属、机械结构），以太能量神秘发光（蓝紫色粒子效果）

**代码位置**：
- 设计文档：docs/DESIGN.md L3-L7
- 模组主类：src/main/java/studio/fantasyit/ether_craft/EtherCraft.java
  - MODID = "ether_craft"
  - id(path) 方法用于创建 Identifier

### 1.2 你的第一步：发现以太（2-3 页）

**内容**：
- 主世界 y=-64~32 挖以太矿石（每区块 4 簇，簇大小 8）
- 下界 y=0~128 也有以太矿石（每区块 2 簇）
- 熔炼以太矿石 -> 以太物品（1 个 = 100 以太能量，可堆叠 64）
- 一组(64 个)以太物品丢出静止约 5 秒 -> 失活以太（基础建材）

**代码位置**：
- 以太矿石方块：src/main/java/studio/fantasyit/ether_craft/block/EtherOreBlock.java
- 以太物品：src/main/java/studio/fantasyit/ether_craft/item/EtherItem.java
- 失活以太转化逻辑：EtherItem.inventoryTick — 检测物品实体静止时间，达到 Config.etherInactivateConvertTick（默认 100 ticks）后转化
- 世界生成配置：src/main/java/studio/fantasyit/ether_craft/worldgen/ 目录
- 配置项：
  - Config.etherConvert = 100（每个以太物品的能量值）
  - Config.etherInactivateConvertTick = 100（转化所需 tick）

### 1.3 制作你的第一台机器（2 页）

**内容**：
- 失活以太合成：
  - 扳手（4 个失活以太）
  - 以太适应节点 LV1（8 个失活以太）
  - 以太加工中心 LV1（8 个失活以太 + 木箱）
- 扳手的作用：旋转机器、安装插件

**代码位置**：
- 扳手物品：src/main/java/studio/fantasyit/ether_craft/item/WrenchItem.java
  - 右键机器方块 -> 旋转朝向 / 安装插件
- 以太适应节点方块：src/main/java/studio/fantasyit/ether_craft/block/node/EtherAdaptNodeBlock.java
  - useItemOn (L91)：扳手交互逻辑
  - useWithoutItem (L151)：打开 GUI
- 以太加工中心方块：src/main/java/studio/fantasyit/ether_craft/block/factory/EtherProcessFactoryBlock.java
  - useWithoutItem (L46)：打开 GUI
- 合成配方：src/main/resources/data/ether_craft/recipe/ 目录下的 JSON 文件
- 数据生成：src/main/java/studio/fantasyit/ether_craft/datagen/ 目录

---

## 第二章：以太适应节点 — 核心机器

### 2.1 节点概述（2 页）

**内容**：
- 以太适应节点是多功能的插件式机器
- 三级：LV1（2 插件槽）、LV2（4 插件槽）、LV3（6 插件槽）
- 三种插件类型：
  - 功能插件 (FUNCTION)：1 个，决定节点核心功能
  - 特性插件 (FEATURE)：最多 6 个，带方向性的次要功能
  - 升级插件 (UPGRADE)：最多 6 个，属性提升

**代码位置**：
- 节点方块实体：src/main/java/studio/fantasyit/ether_craft/block/node/EtherAdaptNodeEntity.java
  - etherStorage：以太存储槽
  - normalStorage：27 格普通物品存储
  - functionStorage：1 格功能插件槽
  - featureUpgradeStorage：6 格特性/升级插件槽
  - tickServer (L150)：核心 tick 逻辑（input -> work -> output）
- 插件管理器：src/main/java/studio/fantasyit/ether_craft/node/NodePluginManager.java
  - ALL_PLUGINS 静态块 (L76-L119)：所有插件的注册
  - PluginInfo(type, id, constructor, predicate, icon)：插件信息记录
- 配置项：
  - Config.nodeDefaultMaxEther = 6400（默认最大以太）
  - Config.nodeUpgradeSlots = [2, 4, 6]（各级别插件槽数）
  - Config.etherStorageMultiplier = 2.5（以太容量升级倍率）

### 2.2 节点 GUI 指南（2-3 页）

**内容**：
- 右键打开节点 GUI
- 顶部标签页：每个已安装的插件一个标签，点击切换
- 主页面显示：以太条、物品存储、插件槽
- 命名功能：点击铅笔按钮可重命名节点

**代码位置**：
- 节点菜单：src/main/java/studio/fantasyit/ether_craft/menu/node/EtherAdaptNodeContainerMenu.java
- 节点屏幕：src/main/java/studio/fantasyit/ether_craft/menu/node/EtherAdaptNodeScreen.java
  - 标签页渲染：TabWidget 按钮，y 偏移 getTopPos() - 21
  - 命名编辑：EditBox + NamePencilButton
- 主页面标签：src/main/java/studio/fantasyit/ether_craft/node/tabs/MainPageProvider.java
- 网络包：src/main/java/studio/fantasyit/ether_craft/network/
  - TriggerSwitchTabC2S：切换标签页
  - SetBlockNameC2S：设置方块名称

### 2.3 安装插件（1-2 页）

**内容**：
- 手持插件物品，副手持扳手，右键节点 -> 安装插件
- 手持扳手右键节点 -> 旋转朝向和特性插件方向
- 插件槽在 GUI 中可见，可手动放入/取出

**代码位置**：
- EtherAdaptNodeBlock.useItemOn (L91-L149)：
  1. 检测主手物品是否匹配某个插件
  2. 检测副手是否为扳手
  3. 调用 NodePluginManager.getInfoFor(stack, type) 查找匹配插件
  4. 安装到对应槽位
  5. 如果只有扳手 -> 旋转朝向和插件方向
- EtherAdaptNodeEntity.updatePluginInfos (L104)：重建插件信息映射
- EtherAdaptNodeEntity.rotatePluginsByAxis (L521)：旋转方向性插件

---

### 2.4 功能插件详解

#### 2.4.1 熔炉以太生成器 / 高炉以太生成器（2 页）

**内容**：
- 匹配物品：熔炉 / 高炉
- 放入燃料 -> 燃烧产生以太
- 熔炉：25 以太/tick，高炉：30 以太/tick
- 燃烧速度 = 物品燃烧时间 / burnTimeFactor（默认 4）

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/function/FunctionFurnaceGenerator.java
  - ID = "generator/furnace", ID_BLAST = "generator/blast"
  - 继承 AbstractItemConsumeFunction
- 配置项：
  - Config.nodeFurnaceBurnTimeFactor = 4
  - Config.nodeFurnaceEtherPerTick = 25
  - Config.nodeBlastFurnaceBurnTimeFactor = 4
  - Config.nodeBlastFurnaceEtherPerTick = 30
- GUI 屏幕：src/main/java/studio/fantasyit/ether_craft/node/tabs/function/ItemConsumeScreen.java
  - 信息面板显示：剩余燃烧秒数、当前以太/最大以太、燃料物品名

#### 2.4.2 切石机以太生成器（1 页）

**内容**：
- 匹配物品：切石机
- 消耗石质物品产生以太

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/function/FunctionStoneGenerator.java
  - ID = "generator/stone"

#### 2.4.3 装备消耗以太生成器（1 页）

**内容**：
- 匹配物品：砂轮
- 消耗附魔装备产生以太，附魔越强越多
- 每个装备燃烧 100 ticks，基础产出 5 + 附魔系数 * 50

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/function/FunctionEquipmentConsumeGenerator.java
  - ID = "generator/equipment"
- 配置项：
  - Config.nodeEquipmentGeneratorBurnTick = 100
  - Config.nodeEquipmentGeneratorCoefficient = 50
  - Config.nodeEquipmentGeneratorBaseAmount = 5
- GUI 屏幕：src/main/java/studio/fantasyit/ether_craft/node/tabs/function/EquipmentConsumeScreen.java
  - 显示装备名、剩余燃烧 tick、当前以太

#### 2.4.4 以太转换器（1 页）

**内容**：
- 匹配物品：龙蛋
- 直接将物品转化为以太（1 物品 = 100 以太，可配置）

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/function/FunctionEtherConverter.java
  - ID = "generator/ether_converter"
- 配置项：Config.nodeEtherConverterCoefficient = 100
- GUI 屏幕：src/main/java/studio/fantasyit/ether_craft/node/tabs/function/EtherConverterScreen.java
  - 显示 "Converting..." 或 "Idle"、转化系数、当前以太

#### 2.4.5 磁铁（2 页）

**内容**：
- 匹配物品：真空管道
- 吸引附近物品到节点
- 可配置吸引范围（中心 X/Y/Z 偏移 + 形状 X/Y/Z 大小）
- 支持物品过滤器（白名单/黑名单）
- 每个物品消耗 100 以太

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/function/FunctionMagnet.java
  - ID = "magnet"
- 配置项：Config.nodeMagnetEtherPerStack = 100
- GUI 屏幕：src/main/java/studio/fantasyit/ether_craft/node/tabs/function/MagnetFunctionScreen.java
  - 6 个滚动条：中心 X/Y/Z（范围 -5~+5）、形状 X/Y/Z（范围 1~6）
  - 过滤器组件

#### 2.4.6 节点加工（2 页）

**内容**：
- 匹配物品：合成器
- 使用 Node Process 配方加工物品
- 配方定义：输入物品列表 + 产出物品 + 以太消耗
- 进度条显示加工进度（最大 100 ticks）

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/function/FunctionNodeProcess.java
  - ID = "node_process"
- 配方类：src/main/java/studio/fantasyit/ether_craft/recipe/node/NodeProcessRecipe.java
  - ingredients：List<SizedIngredient>
  - result：ItemStackTemplate
  - etherCost：int
  - matchesSubset (L46)：检查输入是否包含足够原料
- 配置项：
  - Config.nodeProcessMaxProgress = 100
  - Config.nodeProcessEtherConsumePreUnmatched = 1
- GUI 屏幕：src/main/java/studio/fantasyit/ether_craft/node/tabs/function/FunctionNodeProcessScreen.java
  - 信息面板：进度/最大进度、当前以太、目标物品名
  - 过滤器切换按钮

#### 2.4.7 作物催熟 / 全方块催熟（1 页）

**内容**：
- 匹配物品：骨粉 / 幽匿催发体
- 加速周围作物生长（曼哈顿距离范围，默认 1）
- 每次消耗 500 以太
- 全方块催熟可加速任意可生长方块

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/function/FunctionGrowthAccelerator.java
  - ID = "growth_accelerator", ID_ALL = "growth_accelerator_all"
- 配置项：
  - Config.nodeGrowthAcceleratorEtherCost = 500
  - Config.nodeGrowthAcceleratorRange = 1

#### 2.4.8 以太附魔（2 页）

**内容**：
- 匹配物品：附魔台
- 用以太代替经验附魔
- 三档等级：6000 / 12000 / 24000 以太
- 需要放入可附魔物品
- 进度条显示附魔进度

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/function/FunctionEnchanter.java
  - ID = "enchanter"
- 配置项：
  - Config.nodeEnchanterEtherCosts = [6000, 12000, 24000]
  - Config.nodeEnchanterMaxProgress = 100
- GUI 屏幕：src/main/java/studio/fantasyit/ether_craft/node/tabs/function/EnchanterScreen.java
  - 3 个等级按钮（显示以太消耗）
  - 信息面板：等级+消耗、进度、当前以太
  - 以太条 + 进度条

#### 2.4.9 创造以太生成器（1 页）

**内容**：
- 匹配物品：创造以太
- 创造模式专用，立即填满以太
- 可滑动调节以太量

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/function/FunctionCreativeEther.java
  - ID_FUNC = "generator/creative"
- GUI 屏幕：src/main/java/studio/fantasyit/ether_craft/node/tabs/function/CreativeEtherScreen.java
  - 滚动条调节以太量

#### 2.4.10 自动补给（1 页）

**内容**：
- 匹配物品：以太水晶
- 以太低于阈值（默认 1000）时自动补充
- 每秒补充 5 以太

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/upgrade/EtherAutoSupplyUpgrade.java
  - ID = "ether_auto_supply_upgrade"
- 配置项：
  - Config.etherAutoSupplyThreshold = 1000
  - Config.etherAutoSupplyEtherPerTick = 5

---

### 2.5 特性插件详解

#### 2.5.1 以太流发射器（特性）（2 页）

**内容**：
- 匹配物品：发射器
- 向指定方向发射以太流
- 可设置最低以太阈值（滑动条，范围 0~100000）
- 6 个方向可选（通过方向按钮）

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/feature/FeatureEtherStreamEmitter.java
  - ID = "ether_stream_emitter"
- 配置项：
  - Config.nodeEmitterMinEtherMin = 0
  - Config.nodeEmitterMinEtherMax = 100000
- GUI 屏幕：src/main/java/studio/fantasyit/ether_craft/node/tabs/feature/EtherStreamEmitterScreen.java
  - 继承 DirectionalFilterScreen
  - 6 个方向按钮（UP/NORTH/SOUTH/EAST/WEST/DOWN）
  - 最低以太阈值滚动条

#### 2.5.2 容器交互（2 页）

**内容**：
- 匹配物品：漏斗
- 与相邻容器交互
- 两种模式：插入（向容器放入物品）/ 抽取（从容器取出物品）
- 支持过滤器
- 每个物品消耗 10 以太

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/feature/FeatureContainerInteract.java
  - ID = "container_interact"
- 配置项：Config.nodeContainerInteractEtherPerItem = 10
- GUI 屏幕：src/main/java/studio/fantasyit/ether_craft/node/tabs/feature/ContainerInteractScreen.java
  - 继承 DirectionalFilterScreen
  - 模式切换按钮（Insert/Extract）

#### 2.5.3 投掷器（1 页）

**内容**：
- 匹配物品：投掷器
- 向指定方向投掷物品
- 可设置每次投掷数量（1~64）
- 每个物品消耗 10 以太

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/feature/FeatureDropperThrower.java
  - ID = "dropper_thrower"
- 配置项：Config.nodeDropperThrowerEtherPerItem = 10
- GUI 屏幕：src/main/java/studio/fantasyit/ether_craft/node/tabs/feature/DropperThrowerScreen.java
  - 继承 DirectionalFilterScreen
  - 投掷数量滚动条

#### 2.5.4 红石信号（1 页）

**内容**：
- 匹配物品：比较器
- 输出比较器信号
- 两种模式：以太量模式 / 物品量模式

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/feature/FeatureRedstoneSignal.java
  - ID = "redstone_signal"
- GUI 屏幕：src/main/java/studio/fantasyit/ether_craft/node/tabs/feature/RedstoneSignalTab.java

#### 2.5.5 红石开关 / 红石开关(反转)（1 页）

**内容**：
- 匹配物品：红石粉 / 红石火把
- 根据红石信号启用/禁用节点
- 反转版本逻辑相反

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/feature/RedstoneSwitchUpgrade.java
  - ID = "redstone_switch", ID_REVERT = "redstone_switch_revert"

#### 2.5.6 销毁（1 页）

**内容**：
- 匹配物品：岩浆桶
- 销毁过滤物品
- 两种模式：溢出模式（超过过滤器数量时销毁）/ 全部销毁（匹配即销毁）

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/feature/DestructionUpgrade.java
  - ID = "destruction"
- GUI 屏幕：src/main/java/studio/fantasyit/ether_craft/node/tabs/feature/DestructionTab.java

### 2.6 升级插件详解

#### 2.6.1 存储扩容（1 页）

**内容**：
- 匹配物品：箱子 / 铜箱
- 增加节点内部存储容量

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/upgrade/StorageUpgrade.java
  - ID = "storage_upgrade"

#### 2.6.2 以太容量（1 页）

**内容**：
- 匹配物品：以太皿
- 增加最大以太存储（倍率 2.5）

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/upgrade/EtherStorageUpgrade.java
  - ID = "ether_storage_upgrade"

#### 2.6.3 以太流传输量升级（1 页）

**内容**：
- 匹配物品：运输船(+1) / 漏斗矿车(+2) / 潜影盒(+4)
- 以太流携带物品数量增加

**代码位置**：
- 插件实现：src/main/java/studio/fantasyit/ether_craft/node/plugins/upgrade/EtherStreamStorageUpgrade.java
  - ID = "ether_stream_storage_upgrade" / "_1" / "_2"

#### 2.6.4 以太流能力升级（1 页）

**内容**：
- 防衰减（红石中继器）：防止以太流长距离衰减
- 方块破坏（斧/镐/锹/锄）：以太流可破坏方块
- 实体伤害（武器类）：以太流可伤害实体
- 过滤（纸）：以太流物品过滤
- 作物催熟（骨粉）：以太流加速作物
- 全方块催熟（幽匿催发体）：以太流加速所有方块
- 携带实体（船类）：以太流携带实体飞行
- 携带玩家（矿车）：以太流仅携带玩家
- 速度提升（动力铁轨）：以太流速度x2
- 反弹（粘液球）：以太流碰撞反弹
- 物品显示（物品展示框）：显示携带物品图标
- 文字粒子（成书）：以太流发射文字粒子
- 以太物品化（失活以太）：以太可作为物品传输

**代码位置**：
- 插件实现目录：src/main/java/studio/fantasyit/ether_craft/node/plugins/upgrade/
  - EtherStreamPreventDecayUpgrade.java (ID = "ether_stream_prevent_decay_upgrade")
  - EtherStreamBreakBlockUpgrade.java (ID = "block_breaker_upgrade")
  - EtherStreamDamageUpgrade.java (ID = "damage_upgrade")
  - EtherFilterUpgrade.java (ID = "ether_filter_upgrade")
  - EtherStreamGrowthAcceleratorUpgrade.java (ID = "growth_accelerator_upgrade" / "_allow_all")
  - EtherStreamCarryEntityUpgrade.java (ID = "carry_entity_upgrade" / "carry_player_upgrade")
  - EtherStreamSpeedUpUpgrade.java (ID = "ether_stream_speed_up_upgrade")
  - EtherStreamBounceBackUpgrade.java (ID = "ether_stream_bounce_back_upgrade")
  - EtherStreamDisplayItemUpgrade.java (ID = "ether_stream_display_item_upgrade")
  - EtherStreamTextUpgrade.java (ID = "ether_stream_text_upgrade")
  - EtherItemifyUpgrade.java (ID = "ether_itemify_upgrade")

---

## 第三章：以太加工中心 — 自动化加工

### 3.1 加工中心概述（2 页）

**内容**：
- 基于网格的自动化加工机器
- 四级：LV1(3x3)、LV2(5x5)、LV3(7x7)、LV4(9x9)
- 配方结构：树状加工流程
  - 输入物品（左列）-> 加工步骤（内部网格，芯片定义）-> 输出物品（右列）
- 芯片机制：
  - 每个芯片有 maxEther（容量）、etherDecay（衰减周期）、etherRequire（启动需求）、etherConsume（加工消耗）、maxDurability（耐久度）
  - 芯片通过衰减环机制周期性消耗以太
  - 以太从机器注入芯片，芯片间通过路径传播
  - 耐久度耗尽后芯片销毁
- 压力加成 (Pressure Bonus)：当机器以太总量超过所有芯片容量之和时，加工速度提升（对数增长）
- 泄漏 (Leak)：不匹配的路径（多输入多输出交叉）产生泄漏，消耗额外以太
- 支持多输出：一个配方可产出多个物品

**代码位置**：
- 方块实体：src/main/java/studio/fantasyit/ether_craft/block/factory/EtherProcessFactoryEntity.java
  - tickServer (L267)：更新芯片、tick 芯片行为、处理配方、处理输出
  - updateChips (L125)：读取芯片物品、填充以太、计算 pressureBonus
  - updateRecipe (L193)：匹配工厂布局与配方、计算路径
  - consumeAndPlaceOutput (L413)：消耗输入、放置输出
  - getLevelDef (L111)：获取当前等级定义
- 等级定义：src/main/java/studio/fantasyit/ether_craft/block/factory/FactoryLevelDef.java
  - 网格大小：3x3/5x5/7x7/9x9
  - 输入行：3/5/7/9
  - 加工格：9/25/49/81
  - 输出行：3/5/7/9
- 配方类：src/main/java/studio/fantasyit/ether_craft/recipe/factory/EtherProcessFactoryRecipe.java
  - process：TreeLike<Integer, List<DelayedIngredient>>（配方树）
  - input：List<SizedIngredient>
  - output：List<ItemStackTemplate>
  - matches (L140)：委托 EtherProcessorRecipeUtil.isRecipeCompatible
- 配方工具：src/main/java/studio/fantasyit/ether_craft/recipe/factory/EtherProcessorRecipeUtil.java
- 多步匹配：src/main/java/studio/fantasyit/ether_craft/recipe/factory/multistep/

### 3.2 加工中心 GUI 指南（3-4 页）

**内容**：
- 右键打开加工中心 GUI
- 动态大小：根据等级变化（3x3/5x5/7x7/9x9）
- 左列：输入物品槽
- 中间网格：芯片放置区（彩色角标显示芯片以太状态：绿/黄/橙/红）
- 右列：输出物品槽（空时显示幽灵物品预览）
- 左侧面板：以太条（显示 pressureBonus 和 leak 信息）
- 右侧面板：过滤器切换按钮
- 命名功能：点击铅笔按钮可重命名
- 扳手交互：滚轮切换快速放置芯片槽、拖拽快速放置芯片

**代码位置**：
- 菜单：src/main/java/studio/fantasyit/ether_craft/menu/factory/EtherProcessFactoryContainerMenu.java
- 屏幕：src/main/java/studio/fantasyit/ether_craft/menu/factory/EtherProcessFactoryScreen.java
  - 动态大小：FactoryLevelDef.guiSize()
  - 九宫格背景：MAIN_BG
  - 命名编辑：EditBox 在 (left+5, top+5)，80x12 px，最大 32 字符
  - 芯片网格：ROWS x COLS 在 f.posInternal()，每格 18x18 px
  - 彩色角标：绿/黄/橙/红表示芯片以太水平
  - 进度渲染：绿色覆盖层 0x80c5e1a5，9 段填充算法
  - 输出槽：f.posOutput()，空时显示幽灵物品
  - 过滤器切换：IASwitchButton 在 f.panelRight()
  - 以太条：左侧面板 (lpx+1, lpy+22)，16x2 px
  - 过滤器指示：箭头图标在 f.posFilterMark()
  - 扳手交互：滚轮切换快速放置槽、拖拽快速放置芯片

### 3.3 处理芯片系统（3 页）

**内容**：
- 芯片是加工中心的核心，定义加工步骤
- 13 种芯片，分 4 个阶段 (E1-E4)
- 芯片属性：maxEther、etherDecay、etherRequire、etherConsume、maxDurability
- E1 芯片（基础）：
  - 隔板芯片：基础分路，2x2 失活以太合成
  - 熔炉芯片：加热处理
  - 冲压芯片：冲压成型
  - 切削芯片：切割加工
- E2 芯片（进阶）：
  - 成型芯片：塑形
  - 汇流芯片：能量汇聚
  - 雕刻芯片：精密雕刻
- E3 芯片（高级）：
  - 注能芯片：以太注入
  - 高热芯片：高温处理
  - 焊接芯片：焊接组装
- E4 芯片（终局）：
  - 焕生芯片：生命能量
  - 反物质芯片：反物质
  - 算力芯片：超级计算

**代码位置**：
- 芯片数据文件：src/main/resources/data/ether_craft/ether_process_chip/
  - separator_chip.json（隔板）
  - heating_chip.json（熔炉）
  - stamping_chip.json（冲压）
  - cutting_chip.json（切削）
  - molding_chip.json（成型）
  - converging_chip.json（汇流）
  - carving_chip.json（雕刻）
  - energizing_chip.json（注能）
  - high_heating_chip.json（高热）
  - welding_chip.json（焊接）
  - anima_infusing_chip.json（焕生）
  - antimatter_producing_chip.json（反物质）
  - ai_supercomputing_chip.json（算力）
  - concat_chip.json（连接芯片）
- 芯片物品：src/main/java/studio/fantasyit/ether_craft/item/ProcessChipItem.java
- 芯片行为：src/main/java/studio/fantasyit/ether_craft/recipe/factory/chip/

### 3.4 工厂配方详解（2 页）

**内容**：
- 配方由三部分组成：input[]、process[]、output
- 加工路径：芯片放入内部网格后，系统自动检测从输入到输出的树状路径
- 多个输入流可在加工节点汇聚
- 方案 (Answer) 物品：5x5/7x7/9x9 三种，用于查看配方
- 方案浏览 GUI (AnswerFetchScreen)：翻页浏览所有可用配方

**代码位置**：
- 配方 JSON 文件：src/main/resources/data/ether_craft/recipe/ 目录
- 方案物品：src/main/java/studio/fantasyit/ether_craft/item/AnswerItem.java
- 方案浏览屏幕：src/main/java/studio/fantasyit/ether_craft/menu/grid/answer/AnswerFetchScreen.java
  - 固定大小 140x110 px
  - 翻页按钮：上一页 (80, 82)、下一页 (112, 82)
  - 页码显示 "X / Y" 居中在 (imageWidth/2, 86)
  - 输入槽提示 "select for next"
  - 结果槽显示配方输入/输出
- 查看网格屏幕：src/main/java/studio/fantasyit/ether_craft/menu/grid/ViewGridScreen.java
  - 只读网格显示，居中，每格 18x18 px

### 3.5 工厂升级路径（1 页）

**内容**：
- LV1：8 个失活以太 + 木箱
- LV2：LV1 + 金螺丝 + 失活以太
- LV3：LV2 + 以太锭 x8
- LV4：LV3 + 以太块（19 步复杂加工流程）

**代码位置**：
- 升级配方：src/main/java/studio/fantasyit/ether_craft/recipe/crafting/UpgradeShapedRecipe.java
  - 保留输入物品的组件（用于升级时保留 NBT/数据）
- 方块掉落：EtherProcessFactoryBlock.getDropItem (L55)
  - 根据 LEVEL 属性掉落对应等级物品

---

## 第四章：以太流系统 — 能量与物流

### 4.1 以太流概述（2 页）

**内容**：
- 以太流是携带以太能量的飞行实体（Projectile 子类）
- 基本特性：
  - 自然衰减：随时间消耗以太，速度越快衰减越快
  - 以太玻璃：穿过时减少衰减，用于长距离传输
  - 最大寿命：约 10 分钟（12000 ticks，可配置）
  - 大小与以太量相关：size = 0.03 * log10(ether)
  - 可穿透特定方块（通过 ether_stream_pass_through 标签）
- 三种发射方式：
  - 以太流发射器方块（独立方块）
  - 节点特性插件（以太流发射器）
  - 镀层效果（以太穿行、以太伤害、以太破坏）

**代码位置**：
- 以太流实体：src/main/java/studio/fantasyit/ether_craft/entity/stream/EtherStreamEntity.java
  - tick (L163)：服务端消耗以太、tick 能力、处理碰撞；客户端更新尾部位置
  - onHitBlock (L277)：充能架子方块、检查穿透标签、委托能力、丢弃
  - onHitEntity (L299)：充能镀层物品、转化玻璃物品、委托能力
  - dropAndDiscard (L406)：丢弃前转移剩余以太到相邻方块
  - recreate (L350)：穿过以太玻璃时重新创建
  - shouldPassThrough (L227)：穿透逻辑
  - create (L70)：静态工厂方法
- 以太流发射器方块：src/main/java/studio/fantasyit/ether_craft/block/emitter/EtherStreamEmitterBlock.java
  - FACING 属性
  - useItemOn (L54)：扳手旋转朝向
- 以太流发射器实体：src/main/java/studio/fantasyit/ether_craft/block/emitter/EtherStreamEmitterEntity.java
  - tickServer (L43)：以太 > 1000 时创建 EtherStreamEntity
  - 9 个输入槽，无 GUI
- 配置项：
  - Config.etherStreamMaxTick = 1200
  - Config.etherStreamGlassTransformChance = 0.5
  - Config.etherStreamConsumptionFactor = 0.005
  - Config.etherStreamConsumptionByTimeFactor = 0.0001
  - Config.etherGlassPreventConsume = 20

### 4.2 以太流能力系统（3 页）

**内容**：
- 以太流通过节点升级插件获得能力
- 物品存储：拾取地面物品，碰撞容器/玩家时自动存入
- 方块破坏：用工具破坏方块，支持作物收获留种复种
- 实体伤害：用武器攻击实体（通过假玩家实现）
- 携带实体：拾取并携带实体/玩家飞行，潜行脱离
- 作物催熟：加速作物生长（可升级为加速所有方块）
- 镀层：为物品施加镀层配方
- 反弹：碰撞后反向弹回（不消耗以太）
- 物品显示：显示携带物品的图标
- 成本缩减：降低以太消耗（每级减半）
- 文字粒子：从书籍中发射文字粒子

**代码位置**：
- 能力接口：src/main/java/studio/fantasyit/ether_craft/stream/cap/IStreamCapability.java
- 能力实现目录：src/main/java/studio/fantasyit/ether_craft/stream/cap/
  - EtherStreamStorageCapability.java：物品存储
  - EtherStreamBreakBlockCapability.java：方块破坏
  - EtherStreamDamageCapability.java：实体伤害
  - EtherStreamCarryEntityCapability.java：携带实体
  - EtherStreamGrowthAcceleratorCapability.java：作物催熟
  - EtherStreamBounceBackCapability.java：反弹
  - EtherStreamItemDisplayCapability.java：物品显示
  - EtherStreamCostReducerCapability.java：成本缩减
- 配置项：
  - Config.etherStreamBreakBlockHardnessMultiplier = 20
  - Config.etherStreamBreakBlockEfficiencyDivisor = 3
  - Config.etherStreamBreakBlockConstantCost = 0
  - Config.etherStreamDamageEtherMultiplier = 5
  - Config.etherStreamDamageConstantCost = 0
  - Config.etherStreamGrowthAcceleratorEtherCost = 1000

### 4.3 以太玻璃与以太流管道（1 页）

**内容**：
- 以太玻璃：特殊玻璃，以太流穿过时减少衰减
- 玻璃转化：以太流穿过普通玻璃时有 50% 概率转化为以太玻璃
- 可构建以太玻璃管道网络

**代码位置**：
- 以太玻璃方块：src/main/java/studio/fantasyit/ether_craft/block/EtherGlassBlock.java
- 穿透标签：data/ether_craft/tags/block/ether_stream_pass_through.json
- 配置项：Config.etherStreamGlassTransformChance = 0.5

---

## 第五章：镀层系统 — 装备强化

### 5.1 镀层概述（2 页）

**内容**：
- 为盔甲/物品附加特殊能力
- 镀层流程：
  1. 以太流携带材料 + 目标物品 -> 碰撞触发镀层配方匹配（精确覆盖匹配算法）
  2. 匹配成功 -> 物品进入镀层进行中状态（约 5 秒，显示蓝色粒子）
  3. 以太流持续为物品充能（以太量决定效果强度）
  4. 镀层完成 -> 获得永久效果
- 效果公式：effect = clamp(ether, a1, a2) 线性映射到 a3~a4
  - a1: 最低以太需求
  - a2: 最高以太上限
  - a3: 最低效果值
  - a4: 最高效果值
- 充能方式：以太流碰撞镀层物品/盔甲架/书架/玩家装备时自动分配充能
- 以太耗尽后镀层清除

**代码位置**：
- 镀层配方：src/main/java/studio/fantasyit/ether_craft/recipe/plating/PlatingRecipe.java
  - input：List<SizedIngredient>（所需材料）
  - effectId：Identifier（效果 ID）
  - filter：Ingredient（可镀层物品）
  - values：PlatingEffectFormula（效果公式 a1-a4）
  - matchesFilter (L44)：检查物品是否可镀层
  - matches (L48)：检查材料是否满足配方
  - makeProcessing (L118)：创建 ProgressingPlatingData
- 镀层效果接口：src/main/java/studio/fantasyit/ether_craft/plating/
- 配置项：
  - Config.platingDurationTicks = 100
  - Config.platingMaxEther = 100000

### 5.2 镀层效果详解

#### 5.2.1 战斗类效果（2 页）

**内容**：
- 伤害 (Damage)：攻击实体时消耗以太造成额外伤害，取消原攻击
- 暴击 (Crit)：非跳砍概率暴击
- 暴击伤害 (Crit Damage)：暴击时提高暴击倍率
- 猎头 (Head Hunt)：击杀时概率掉落生物头颅
- 格挡 (Block)：右键减免正面伤害 50% 和击退
- 伦理 (Ethic)：攻击被动生物不掉血直接掉落产物，中立生物取消仇恨

**代码位置**：
- 效果实现目录：src/main/java/studio/fantasyit/ether_craft/plating/effects/
  - DamagePlatingEffect.java（伤害，ID = "damage"）
  - CritPlatingEffect.java（暴击，ID = "crit"）
  - CritDamagePlatingEffect.java（暴击伤害，ID = "crit_damage"）
  - HeadHuntPlatingEffect.java（猎头，ID = "head_hunt"）
  - BlockPlatingEffect.java（格挡，ID = "block"）
  - EthicPlatingEffect.java（伦理，ID = "ethic"）

#### 5.2.2 移动类效果（2 页）

**内容**：
- 冲刺 (Dash)：右键/按键向前冲刺，有冷却
- 高跳 (High Jump)：右键/按键高跳，有冷却
- 无重力 (No Gravity)：射箭时箭矢不受重力
- 郊狼时间 (Coyote Time)：离开方块边缘后短暂可跳
- 以太穿行 (Ether Stream Dash)：右键化为以太流高速穿行，穿透实体
- 静步 (Silent Step)：移动不产生振动，不惊动监守者

**代码位置**：
- DashPlatingEffect.java（冲刺，ID = "dash"）
- HighJumpPlatingEffect.java（高跳，ID = "high_jump"）
- NoGravityPlatingEffect.java（无重力，ID = "no_gravity"）
- CoyoteTimePlatingEffect.java（郊狼时间，ID = "coyote_time"）
- EtherStreamDashPlatingEffect.java（以太穿行，ID = "ether_stream_dash" / "ether_stream_dash_faster"）
- SilentStepPlatingEffect.java（静步，ID = "silent_step"）

#### 5.2.3 生存/辅助类效果（2 页）

**内容**：
- 伪装 (Camouflage)：站立不动时隐身+诱饵箱+清除仇恨+回复以太
- 抗黑暗 (Anti-Darkness)：免疫黑暗/失明，夜视
- 抗音爆 (Anti-Sonic Boom)：免疫监守者声波
- 耐久吸收 (Durability Absorption)：消耗以太代替装备耐久
- 破坏进包 (Break->Inv)：破坏方块掉落物直接进背包
- 击杀进包 (Kill->Inv)：击杀掉落物直接进背包
- 吃废石 (Stone Absorb)：破坏石质方块转化为以太

**代码位置**：
- CamouflagePlatingEffect.java（伪装，ID = "camouflage"）
- AntiDarknessPlatingEffect.java（抗黑暗，ID = "anti_darkness"）
- AntiSonicBoomPlatingEffect.java（抗音爆，ID = "anti_sonic_boom"）
- DurabilityAbsorptionPlatingEffect.java（耐久吸收，ID = "durability_absorption"）
- BreakToInvPlatingEffect.java（破坏进包，ID = "break_to_inv"）
- KillToInvPlatingEffect.java（击杀进包，ID = "kill_to_inv"）
- StoneAbsorbPlatingEffect.java（吃废石，ID = "stone_absorb"）

#### 5.2.4 以太流攻击类效果（1 页）

**内容**：
- 以太伤害 (Ether Stream Damage)：右键发射以太流攻击实体
- 以太破坏 (Ether Stream Break)：右键发射以太流破坏方块
- 追踪 (Tracking)：射箭时箭矢轻微追踪附近生物

**代码位置**：
- EtherStreamDamagePlatingEffect.java（以太伤害，ID = "ether_stream_damage"）
- EtherStreamBreakPlatingEffect.java（以太破坏，ID = "ether_stream_break"）
- TrackingPlatingEffect.java（追踪，ID = "tracking"）

---

## 第六章：终局之路 — 三大分支

### 6.1 全自动化工厂（2 页）

**内容**：
- 搭建完整加工流水线，自动处理各种材料
- 关键组件：
  - 以太加工中心 LV4（9x9 网格）
  - 以太适应节点 LV3（6 插件槽，用于原料供应和产物收集）
  - 以太流系统（物流传输）
  - 容器交互插件（自动输入/输出）
- 示例流程：矿石 -> 以太转换器 -> 以太 -> 加工中心 -> 成品 -> 容器交互输出

**代码位置**：
- 以太加工中心 LV4：EtherProcessFactoryEntity + FactoryLevelDef (9x9)
- 以太适应节点 LV3：EtherAdaptNodeEntity + Config.nodeUpgradeSlots[2] = 6
- 容器交互插件：FeatureContainerInteract.java
- 以太转换器：FunctionEtherConverter.java

### 6.2 强大个人装备（2 页）

**内容**：
- 集齐所有镀层效果，成为超人
- 推荐组合：
  - 战斗套：伤害 + 暴击 + 暴击伤害 + 猎头 + 格挡
  - 移动套：冲刺 + 高跳 + 以太穿行 + 静步
  - 生存套：伪装 + 抗黑暗 + 抗音爆 + 耐久吸收
  - 便利套：破坏进包 + 击杀进包 + 吃废石
- 以太流充能：使用以太流发射器持续为装备充能

**代码位置**：
- 所有镀层效果：src/main/java/studio/fantasyit/ether_craft/plating/effects/
- 以太流充能逻辑：EtherStreamEntity.onHitEntity (L299)
- 镀层配方 JSON：src/main/resources/data/ether_craft/recipe/plating/

### 6.3 以太流网络（2 页）

**内容**：
- 以太玻璃管道 + 反弹 + 携带实体/玩家，覆盖基地的物流/能量网络
- 关键组件：
  - 以太玻璃管道：减少衰减
  - 反弹升级：以太流碰撞后反向弹回
  - 携带实体/玩家：以太流携带物品/生物/玩家飞行
  - 速度提升：以太流速度 x2
  - 防衰减：防止长距离衰减
- 应用场景：
  - 物品运输网络
  - 玩家快速移动系统
  - 自动化农场（作物催熟 + 收集）

**代码位置**：
- 以太玻璃：EtherGlassBlock.java
- 反弹能力：EtherStreamBounceBackCapability.java
- 携带实体能力：EtherStreamCarryEntityCapability.java
- 速度提升：EtherStreamSpeedUpUpgrade.java
- 防衰减：EtherStreamPreventDecayUpgrade.java

---

## 附录 A：配置参考

**文件位置**：src/main/java/studio/fantasyit/ether_craft/Config.java

### 核心以太机制
| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| etherConvert | 100 | 每个以太物品的能量值 |
| etherInactivateConvertTick | 100 | 以太物品丢出静止后转化为失活以太的 tick 数 |

### 以太适应节点
| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| nodeDefaultMaxEther | 6400 | 节点默认最大以太 |
| nodeUpgradeSlots | [2, 4, 6] | 各级别插件槽数 |
| etherStorageMultiplier | 2.5 | 以太容量升级倍率 |

### 节点功能插件
| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| nodeFurnaceBurnTimeFactor | 4 | 熔炉燃烧时间除数 |
| nodeFurnaceEtherPerTick | 25 | 熔炉每 tick 产以太 |
| nodeBlastFurnaceBurnTimeFactor | 4 | 高炉燃烧时间除数 |
| nodeBlastFurnaceEtherPerTick | 30 | 高炉每 tick 产以太 |
| nodeMagnetEtherPerStack | 100 | 磁铁每物品消耗以太 |
| nodeContainerInteractEtherPerItem | 10 | 容器交互每物品消耗 |
| nodeDropperThrowerEtherPerItem | 10 | 投掷器每物品消耗 |
| nodeProcessMaxProgress | 100 | 节点加工最大进度 |
| nodeProcessEtherConsumePreUnmatched | 1 | 未匹配时每 tick 消耗 |
| nodeEtherConverterCoefficient | 100 | 以太转换器系数 |
| nodeEquipmentGeneratorBurnTick | 100 | 装备燃烧 tick |
| nodeEquipmentGeneratorCoefficient | 50 | 装备生成系数 |
| nodeEquipmentGeneratorBaseAmount | 5 | 装备基础产出 |
| nodeGrowthAcceleratorEtherCost | 500 | 作物催熟以太消耗 |
| nodeGrowthAcceleratorRange | 1 | 作物催熟曼哈顿范围 |
| nodeEmitterMinEtherMin | 0 | 发射器最低以太下限 |
| nodeEmitterMinEtherMax | 100000 | 发射器最低以太上限 |
| nodeEnchanterEtherCosts | [6000, 12000, 24000] | 附魔三档以太消耗 |
| nodeEnchanterMaxProgress | 100 | 附魔最大进度 |
| etherAutoSupplyThreshold | 1000 | 自动补给触发阈值 |
| etherAutoSupplyEtherPerTick | 5 | 自动补给每 tick 量 |

### 以太流
| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| etherStreamMaxTick | 1200 | 以太流最大寿命 (tick) |
| etherStreamGlassTransformChance | 0.5 | 玻璃转化概率 |
| etherStreamConsumptionFactor | 0.005 | 基础消耗系数 |
| etherStreamConsumptionByTimeFactor | 0.0001 | 时间消耗系数 |
| etherGlassPreventConsume | 20 | 玻璃防消耗值 |
| etherStreamBreakBlockHardnessMultiplier | 20 | 破坏方块硬度倍率 |
| etherStreamBreakBlockEfficiencyDivisor | 3 | 效率附魔除数 |
| etherStreamBreakBlockConstantCost | 0 | 破坏方块固定消耗 |
| etherStreamDamageEtherMultiplier | 5 | 伤害以太倍率 |
| etherStreamDamageConstantCost | 0 | 伤害固定消耗 |
| etherStreamGrowthAcceleratorEtherCost | 1000 | 流催熟以太消耗 |

### 镀层
| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| platingDurationTicks | 100 | 镀层持续时间 |
| platingMaxEther | 100000 | 镀层最大以太 |

---

## 附录 B：进度线速查

### E1 — 发现以太
- 挖以太矿 -> 熔炼 -> 以太物品
- 64 个丢出静止 -> 失活以太
- 失活以太合成：扳手、LV1 节点、LV1 工厂、隔板/熔炉/冲压/切削芯片
- LV1 工厂加工：刀刃、金螺丝、钻石针、以太皿、以太水晶

### E2 — 基础加工与以太流
- 高级芯片：雕刻、成型
- 工厂加工：汇流芯片、注能芯片、真空管道
- 以太流发射器、LV2 节点、LV2 工厂、以太玻璃
- 以太流升级插件可用

### E3 — 以太锭与镀层
- 以太锭合成（工厂加工，多通道）
- 以太块（9 个以太锭）
- 高热/焊接芯片
- LV3 工厂、LV3 节点
- 镀层系统

### E4 — 终局
- 顶级芯片：焕生、AI、反物质
- LV4 工厂
- 三大分支

---

## 附录 C：术语速查

| 术语 | 说明 |
|------|------|
| 以太 (Ether) | 模组核心能量元素 |
| 失活 (Inactivation) | 以太 -> 失活以太的转化 |
| 压力加成 (Pressure Bonus) | 以太过剩时加工速度提升 |
| 泄漏 (Leak) | 路径不匹配消耗额外以太 |
| 衰减 (Decay) | 以太流/芯片随时间的消耗机制 |
| 镀层进行中 (Plating In Progress) | 物品镀层未完成的状态 |
| 方案 (Answer) | 存储配方数据的查看物品 |
| 过滤器 (Filter) | 物品过滤配置 |
| 白名单 (Whitelist) | 仅允许匹配物品 |
| 黑名单 (Blacklist) | 禁止匹配物品 |
| 溢出模式 (Overflow Mode) | 物品超过过滤器时销毁 |
| 全部销毁 (Destroy All Mode) | 匹配即销毁 |
