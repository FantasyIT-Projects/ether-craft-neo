# 以太工艺 — 代码架构文档

本文档将 DESIGN.md 中的每个概念映射到对应的代码框架，细化到子模块、核心类、数据流和设计要点。

---

## 一、以太能量系统

### 1.1 以太容器 (EtherContainer)

**核心接口**: `block/base/EtherContainer.java`

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 接口定义 | `EtherContainer.java` | 定义以太存取的标准接口: getEther/getMaxEther/setEther/receiveEther/extractEther |
| 附件存储 | `AttachmentDataRegistry.java` | ETHER_CONTAINER / ETHER_CONTAINER_MAX 数据附件 |
| BlockCapability | `CapabilityRegistry.java` | 注册 ETHER_CONTAINER 为方块能力 |
| 基类实现 | `BaseEtherContainerBlockEntity.java` | 继承 BlockEntity + 实现 EtherContainer + ResourceHandler |
| 以太槽位 | `menu/base/ether/EtherSlotContainer.java` | 将以太暴露为物品槽位供传输 |

**数据流**:
```
以太物品插入 -> BaseEtherContainerBlockEntity.insert()
  -> 识别为 ETHER -> receiveEther(amount * 100)
  -> setData(ETHER_CONTAINER, newValue)
  -> 校验 maxEther -> 同步客户端
```

### 1.2 以太物品与失活

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 物品注册 | `ItemRegistry.java` | ETHER, INACTIVATED_ETHER, ETHER_CREATIVE |
| 方块注册 | `BlockRegistry.java` | ETHER_BLOCK, ETHER_ORE, DEEPSLATE_ETHER_ORE, NETHER_ETHER_ORE |
| 失活转化 | `event/ItemEntityTickEvent.java` | 监听 EntityTickEvent, 满堆叠静止后转化 |
| 转化数据 | `attachment/EtherInactivateConvertData.java` | SavedData, 追踪物品实体静止 tick 计数 |
| 配置 | `Config.java` | etherConvert(1个=100), etherInactivateConvertTick |

**设计要点**:
- 失活转化使用 SavedData 持久化, 服务端重启不丢失进度
- 以太物品插入机器时直接转化为能量数值, 不占用物品槽位
- EtherContainer 使用 NeoForge Attachment 系统而非 NBT

### 1.3 世界生成

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 数据生成 | `datagen/WorldGenData.java` | 配置矿脉特征、放置、生物群系修饰 |
| 矿脉配置 | 同上 | 主世界 y=-64~32 4簇/区块, 下界 y=0~128 2簇/区块, 簇大小8 |

---

## 二、以太适配节点 (Ether Adapt Node)

### 2.1 方块与方块实体

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 方块定义 | `block/node/EtherAdaptNodeBlock.java` | 方块属性、FACING/LEVEL 状态、右键交互 |
| 方块实体 | `block/node/EtherAdaptNodeEntity.java` | 核心逻辑: 插件管理、以太存储、物品处理、网络同步 |
| 方块物品 | `item/EtherAdaptNodeBlockItem.java` | 多等级 BlockItem, tooltip 显示已安装插件 |
| 注册 | BlockRegistry/ItemRegistry/BlockEntityRegistry | 注册 1个方块 + 3个物品变体 + 实体类型 |

**EtherAdaptNodeEntity 内部结构**:
```
EtherAdaptNodeEntity extends BlockEntity
  implements ResourceHandler<ItemResource>, EtherContainer, ITickable, IWorldRenderBE
  |-- nodeProperty: NodeProperty           // 可修改属性
  |-- etherStorage: EtherSlotSyncContainer // 以太存储槽
  |-- functionStorage: EtherPluginUpgradeContainer  // 功能插件(1槽)
  |-- featureUpgradeStorage: EtherPluginUpgradeContainer // 特性/升级插件(6槽)
  |-- normalStorage: RangeLimitPlaceContainer  // 通用物品存储(27槽)
  |-- normalStorageFilter: ItemFilter       // 物品过滤器
  |-- functionPlugin: InstalledPlugin       // 当前功能插件
  |-- featureAttachedDirection: Map<Direction, InstalledPlugin> // 方向映射
  |-- syncedPluginData: Map<InstalledPlugin, Map<Identifier, Integer>> // 同步数据
```

**tick 流程**:
```
tickServer()
  -> functionStorage.preTick() + featureUpgradeStorage.preTick()
  -> functionStorage.tickInput() + featureUpgradeStorage.tickInput()
  -> functionStorage.tickWork() + featureUpgradeStorage.tickWork()
  -> functionStorage.tickOutput() + featureUpgradeStorage.tickOutput()
  -> ticket.tick()
  -> if markUpdate: updateProperty() + updatePluginInfos()
```

### 2.2 插件系统核心

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 插件管理器 | `node/NodePluginManager.java` | 插件注册、匹配、构造 |
| 插件基类 | `node/plugins/base/AbstractNodePlugin.java` | 插件生命周期接口 |
| 插件类型 | `NodePluginManager.PluginType` | FUNCTION / FEATURE / UPGRADE / DUMMY |
| 插件信息 | `NodePluginManager.PluginInfo` | 类型、ID、构造器、匹配谓词、图标 |
| 已安装插件 | `node/plugins/InstalledPlugin.java` | 类型、槽位索引、插件ID |
| 节点属性 | `node/NodeProperty.java` | maxEther, slotUnlock, streamMaxStorage 等 |
| 插件容器 | `block/node/EtherPluginUpgradeContainer.java` | 管理插件安装/卸载/生命周期 |

**AbstractNodePlugin 可重写方法**:

| 方法 | 用途 |
|------|------|
| modifyNodeProperty(NodeProperty) | 修改节点属性 |
| tick() / tickInput() / tickWork() / tickOutput() | 分阶段 tick |
| inputFilter(ItemResource) / outputFilter(ItemResource) | 物品过滤 |
| earlyHandleInput(ItemResource, int, TransactionContext) | 前置物品拦截 |
| handleOverflow(ItemResource, int, TransactionContext) | 溢出处理 |
| registerSlots(EtherAdaptNodeContainerMenu) | 注册菜单槽位 |
| syncScreenData(SyncScreenDataC2S) | 客户端->服务端数据同步 |
| saveAdditional(ValueOutput) / loadAdditional(ValueInput) | 持久化 |
| onDestroy() | 插件移除回调 |
| onWrenchRotate(Direction.Axis) | 扳手旋转回调 |

**插件注册流程** (NodePluginManager.collect()):
```java
registerPlugin(PluginType.FUNCTION, ID, Constructor::new, 匹配物品);
registerPlugin(PluginType.FEATURE, ID, Constructor::new, 匹配物品);
registerPlugin(PluginType.UPGRADE, ID, Constructor::new, 匹配物品);
```

### 2.3 功能插件 (FUNCTION)

**目录**: `node/plugins/function/`

| 插件 | 文件 | 核心逻辑 |
|------|------|---------|
| 熔炉/高炉发电机 | FunctionFurnaceGenerator.java | extends AbstractItemConsumeFunction, 燃烧燃料产以太 |
| 切石机发电机 | FunctionStoneGenerator.java | extends AbstractItemConsumeFunction, 消耗石头产以太 |
| 装备消耗发电机 | FunctionEquipmentConsumeGenerator.java | extends AbstractItemConsumeFunction, 消耗附魔装备 |
| 以太转换器 | FunctionEtherConverter.java | earlyHandleInput 拦截物品直接转以太 |
| 磁铁 | FunctionMagnet.java | tickInput 扫描范围内 ItemEntity 吸入 |
| 节点加工 | FunctionNodeProcess.java | tickWork 匹配 NodeProcessRecipe 加工 |
| 作物催熟 | FunctionGrowthAccelerator.java | tickWork 扫描周围方块 randomTick |
| 附魔台 | FunctionEnchanter.java | tickWork 用以太附魔物品 |
| 创造模式 | FunctionCreativeEther.java | tickWork 直接填满以太 |
| 抽象基类 | AbstractItemConsumeFunction.java | 燃烧类插件基类(燃烧进度、材料类型) |

### 2.4 特性插件 (FEATURE)

**目录**: `node/plugins/feature/`

| 插件 | 文件 | 核心逻辑 |
|------|------|---------|
| 以太流发射器 | FeatureEtherStreamEmitter.java | tickOutput 创建 EtherStreamEntity |
| 投掷器 | FeatureDropperThrower.java | tickOutput 向方向投掷物品 |
| 容器交互 | FeatureContainerInteract.java | tickInput/tickOutput 与相邻容器传输 |
| 红石信号 | FeatureRedstoneSignal.java | 输出比较器信号 |
| 红石开关 | RedstoneSwitchUpgrade.java | 根据红石启用/禁用 |
| 销毁 | DestructionUpgrade.java | 销毁过滤物品 |
| 方向性基类 | AbstractDirectionalFeature.java | 带方向的特性插件基类 |
| 方向性过滤基类 | AbstractDirectionalFilterFeature.java | 带方向+过滤的特性插件基类 |

### 2.5 升级插件 (UPGRADE)

**目录**: `node/plugins/upgrade/`

| 插件 | 文件 | 核心逻辑 |
|------|------|---------|
| 存储扩容 | StorageUpgrade.java | modifyNodeProperty 增加 slotUnlock |
| 以太容量 | EtherStorageUpgrade.java | modifyNodeProperty 增加 maxEther |
| 以太流存储+1/+2/+4 | EtherStreamStorageUpgrade.java | 实现 IEtherStreamCapabilityProviderPlugin |
| 防衰减 | EtherStreamPreventDecayUpgrade.java | 实现 IEtherStreamCapabilityProviderPlugin |
| 方块破坏 | EtherStreamBreakBlockUpgrade.java | 实现 IEtherStreamCapabilityProviderPlugin |
| 实体伤害 | EtherStreamDamageUpgrade.java | 实现 IEtherStreamCapabilityProviderPlugin |
| 过滤 | EtherFilterUpgrade.java | 实现 IEtherStreamCapabilityProviderPlugin |
| 作物催熟 | EtherStreamGrowthAcceleratorUpgrade.java | 实现 IEtherStreamCapabilityProviderPlugin |
| 携带实体/玩家 | EtherStreamCarryEntityUpgrade.java | 实现 IEtherStreamCapabilityProviderPlugin |
| 速度提升 | EtherStreamSpeedUpUpgrade.java | 实现 IEtherStreamCapabilityProviderPlugin |
| 反弹 | EtherStreamBounceBackUpgrade.java | 实现 IEtherStreamCapabilityProviderPlugin |
| 物品显示 | EtherStreamDisplayItemUpgrade.java | 实现 IEtherStreamCapabilityProviderPlugin |
| 自动补给 | EtherAutoSupplyUpgrade.java | tickWork 低以太时自动补充 |
| 以太物品化 | EtherItemifyUpgrade.java | modifyNodeProperty 设置 itemifyEther |
| 文字粒子 | EtherStreamTextUpgrade.java | 实现 IEtherStreamCapabilityProviderPlugin |
| 镀层 | EtherStreamPlatingUpgrade.java | 实现 IEtherStreamCapabilityProviderPlugin |

**IEtherStreamCapabilityProviderPlugin 接口**:
```java
interface IEtherStreamCapabilityProviderPlugin {
    void provideCapabilities(IEtherStreamLike stream);
}
```
升级插件通过此接口在以太流创建时注入能力。

### 2.6 GUI 系统

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 容器菜单 | `menu/node/EtherAdaptNodeContainerMenu.java` | 节点 GUI 容器 |
| 标签页管理 | `node/EtherAdaptNodeUpgradeTabManager.java` | 管理插件的 GUI 标签页 |
| 标签页基类 | `node/tabs/BaseEtherNodeTabWidgetProvider.java` | 标签页控件基类 |
| 主页面 | `node/tabs/MainPageProvider.java` | 以太条、插件槽显示 |
| 物品消耗页 | `node/tabs/function/ItemConsumeScreen.java` | 燃烧进度、以太显示 |
| 节点加工页 | `node/tabs/function/NodeProcessScreen.java` | 加工进度、输入输出 |
| 过滤器页 | `node/tabs/feature/DirectionalFilterScreen.java` | 方向过滤配置 |
| 磁铁页 | `node/tabs/function/MagnetScreen.java` | 范围配置 |
| 附魔页 | `node/tabs/function/EnchanterScreen.java` | 附魔等级选择 |
| 过滤器GUI | `node/filter/FilterGuiRegCommon.java` | 通用过滤器控件注册 |

### 2.7 渲染系统

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 渲染管理器 | `node/PluginRenderManager.java` | 插件渲染器注册与调度 |
| 渲染接口 | `PluginRenderManager.PluginRender` | render(face, dTick, entity, state) |
| 方块渲染器 | `client/...` (通过 SpecialRendererRegister) | 节点方块模型渲染 |
| 纹理图集 | `EtherAdapterNodeAtlas` | UV 坐标管理 |

---

## 三、以太加工中心 (Ether Process Factory)

### 3.1 方块与方块实体

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 方块定义 | `block/factory/EtherProcessFactoryBlock.java` | 方块属性、LEVEL 状态 |
| 方块实体 | `block/factory/EtherProcessFactoryEntity.java` | 核心逻辑: 网格管理、配方匹配、芯片管理、加工进度 |
| 方块物品 | `item/EtherProcessFactoryBlockItem.java` | 多等级 BlockItem, tooltip 显示内容物 |
| 等级定义 | `factory/FactoryLevelDef.java` | 四个等级的网格尺寸、GUI 布局 |

**EtherProcessFactoryEntity 内部结构**:
```
EtherProcessFactoryEntity extends BaseEtherContainerBlockEntity
  implements EtherContainer, MenuProvider, ITickable, IWorldRenderBE
  |-- ROWS, COLS: int                    // 网格尺寸
  |-- slotChips: EtherProcessWorkingChip[][] // 每个格子的芯片实例
  |-- processingRecipes: EtherProcessFactoryRecipe[] // 每行当前配方
  |-- processingInputs: EtherFactoryRecipeInput[]    // 每行配方输入
  |-- processingProgress: int[]          // 每行加工进度
  |-- currentEther: int[][]              // 每个格子当前以太(渲染用)
  |-- pathBelongings: int[][]            // 路径归属(渲染用)
  |-- pathDepth: int[][]                 // 路径深度(渲染用)
  |-- pathDirection: int[][]             // 路径方向(渲染用)
  |-- pressureBonus: int                 // 压力加成
  |-- leak: int                          // 泄漏量
  |-- filters: ItemFilter[]              // 每行输入过滤器
  |-- possibleResults: SimpleContainer   // 每行预期输出(渲染用)
```

**tick 流程**:
```
tickServer()
  -> updateChips()          // 更新芯片状态、以太分配、压力计算
  -> tickChipBehaviors()    // 执行芯片特殊行为
  -> 遍历 processingRecipes:
      if progress < MAX: progress += pressureBonus
      else: consumeAndPlaceOutput()
  -> if markUpdate: updateRecipe()  // 重新匹配配方
  -> extractEther(leak * 20 * pressureBonus)  // 泄漏消耗
```

### 3.2 芯片系统

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 工作芯片 | `factory/EtherProcessWorkingChip.java` | 芯片运行时实例: 以太存储、衰减环、耐久度、消耗 |
| 芯片管理器 | `factory/EtherProcessChipManager.java` | 芯片数据管理、行为注册 |
| 芯片数据加载 | `datapack/ProcessChipDataLoader.java` | 从数据包加载芯片定义 |
| 芯片行为接口 | `factory/IProcessChipBehavior.java` | 芯片特殊行为接口 |
| 芯片物品 | `item/ProcessChipItem.java` | 芯片物品类, 数据组件驱动 |
| 数据组件 | `DataComponentRegistry.java` | CHIP_ID, DURABILITY, CONVERSION_COUNTER |

**EtherProcessWorkingChip 核心机制**:
```
衰减环 (decayCircle):
  - 长度为 etherDecay 的环形缓冲区
  - 每次 addEther 写入当前位置
  - 每次 tick 从当前位置读取并扣除
  - 实现周期性以太衰减

加工消耗 (consume):
  - 从衰减环中由新到旧扣除 etherConsume 量
  - 剩余不足部分从 ether 直接扣除
  - 消耗后检查耐久度

耐久度:
  - maxDurability > 0 时启用
  - 每次加工后 damage(1)
  - 耐久归零 -> destroyed = true -> 芯片销毁

压力加成:
  - 当机器以太 > 所有芯片容量之和时
  - pressureBonus = log2(剩余倍数 + 1)
  - 加工速度 = 基础速度 * pressureBonus
```

### 3.3 配方系统

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 配方类型 | `recipe/factory/EtherProcessFactoryRecipe.java` | 树状配方定义, 输入/加工/输出 |
| 配方JSON | `recipe/factory/EtherProcessRecipeJson.java` | JSON 序列化: InputEntry/ProcessEntry/OutputEntry |
| 配方输入 | `recipe/factory/EtherFactoryRecipeInput.java` | 运行时配方输入: 物品、树、路径 |
| 配方匹配 | `util/EtherProcessorRecipeUtil.java` | 树匹配算法: 路径检测、配方兼容性验证 |
| 配方管理 | `factory/EtherProcessRecipeManager.java` | 配方查询、额外配方提供者 |
| 额外配方 | `factory/ExtraRecipeProvider.java` | 动态配方接口(如熔炉配方转工厂配方) |
| 延迟原料 | `recipe/DelayedIngredient.java` | 延迟解析的原料(支持 SizedIngredient 和标签) |
| 原料序列化 | `recipe/IngredientSerializer.java` | 芯片原料的 JSON 序列化 |

**配方 JSON 结构**:
```json
{
  "type": "ether_craft:ether_process",
  "input": [
    { "id": "I0", "item": "原料", "next": "P2" }
  ],
  "process": [
    { "id": "P2", "item": [{ "chip": "ether_craft:heating_chip" }], "next": "P1" },
    { "id": "P1", "item": [{ "chip": "ether_craft:stamping_chip" }], "next": "O" }
  ],
  "output": {
    "id": "O",
    "item": [{ "id": "产出物品", "count": 1 }]
  }
}
```

**配方匹配算法** (EtherProcessorRecipeUtil):
```
1. processFactoryInput(): 扫描网格, 检测树状路径
   - markTreeArea(): BFS 标记无环通路
   - scanForTrees(): 沿通路生成加工树
   - 多输入多输出交叉 -> 泄漏

2. isRecipeCompatible(): 验证加工树是否匹配配方树
   - 对每个输入节点, 检查物品是否匹配配方输入
   - 对每个加工节点, 检查芯片是否匹配配方步骤
   - 使用全排列匹配 + 二分图匹配验证
```

### 3.4 GUI 系统

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 容器菜单 | `menu/factory/EtherProcessFactoryContainerMenu.java` | 工厂 GUI 容器 |
| 屏幕 | `client/...` (通过 ClientGuiRegistry) | 客户端渲染 |
| 方案物品 | `item/EtherProcessRecipeAnswerItem.java` | 存储和查看配方的物品 |
| 方案GUI | `item/ViewGridScreenCreator.java` | 查看配方的 GUI |

---

## 四、以太流系统 (Ether Stream)

### 4.1 核心实体

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 实体定义 | `entity/stream/EtherStreamEntity.java` | 以太流实体: 运动、碰撞、衰减、能力管理 |
| 实体注册 | `EntityRegistry.java` | ETHER_STREAM_ENTITY |
| 接口定义 | `stream/IEtherStreamLike.java` | 以太流通用接口 |
| 位置方向 | `stream/PosDir.java` | 位置+方向的不可变记录 |
| 消耗计算 | `stream/EtherConsumer.java` | 以太消耗计算: 基础因子+时间因子+能力消耗 |

**EtherStreamEntity 内部结构**:
```
EtherStreamEntity extends Projectile implements IEtherStreamLike
  |-- ether: int                          // 当前以太量
  |-- realCanReceiveEther: int            // 实际可接收上限
  |-- capabilities: List<IStreamCapability> // 附加能力列表
  |-- consumer: EtherConsumer             // 消耗计算器
  |-- tailX/Y/Z[MAX_TAIL]: double[]       // 尾部轨迹(渲染用)
  |-- toSyncData: List<IEtherStreamSyncedData> // 同步数据
```

**生命周期**:
```
create(level, ether, pos, motion)
  -> firstTick()              // 初始化能力
  -> 每 tick:
      capabilities.tick()
      consumer.getTotalConsumption(ether, tickCount) -> consumeEther()
      if ether <= 0 || tickCount >= maxTick -> dropAndDiscard()
      fastHit() -> onHit() -> onHitBlock()/onHitEntity()
      move()
  -> dropAndDiscard():
      capabilities.onBeforeDestroy()
      碰撞方块 -> EtherContainer.receiveEther()
      capabilities.onDestroy()
      discard()
```

### 4.2 以太流发射器

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 独立方块 | `block/emitter/EtherStreamEmitterBlock.java` | 可旋转方向, 扳手交互 |
| 方块实体 | `block/emitter/EtherStreamEmitterEntity.java` | 以太>1000 时自动发射 |
| 节点插件 | `node/plugins/feature/FeatureEtherStreamEmitter.java` | 节点内嵌发射器, 可设最低阈值 |

**发射流程**:
```
EtherStreamEmitterEntity.tickServer():
  if ether > 1000:
    VirtualEtherStreamHolderManager.canCreateStream(posDir)?
    -> createStream(level, posDir, ether, pos, motion)
    -> 添加 EtherStreamStorageCapability
    -> 转移物品栏物品到流
    -> setEther(0)

FeatureEtherStreamEmitter.tickOutput():
  if ether >= minEther:
    -> 创建流
    -> 所有插件通过 IEtherStreamCapabilityProviderPlugin 注入能力
    -> 转移过滤后的物品
```

### 4.3 流能力系统 (Stream Capabilities)

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 能力接口 | `stream/cap/IStreamCapability.java` | 能力生命周期: tick/hitEntity/hitBlock/onDestroy |
| 能力工厂 | `stream/CapabilityFactoryManager.java` | 能力注册、创建、序列化 |
| 物品存储 | `stream/cap/EtherStreamStorageCapability.java` | 携带物品, 拾取地面物品, 存入容器 |
| 方块破坏 | `stream/cap/EtherStreamBreakBlockCapability.java` | 用工具破坏方块, 支持作物收获留种 |
| 实体伤害 | `stream/cap/EtherStreamDamageCapability.java` | 用武器攻击实体(假玩家实现) |
| 携带实体 | `stream/cap/EtherStreamCarryEntityCapability.java` | 拾取携带实体/玩家, 潜行脱离 |
| 作物催熟 | `stream/cap/EtherStreamGrowthAcceleratorCapability.java` | 加速作物/所有方块 |
| 镀层 | `stream/cap/EtherStreamPlatingCapability.java` | 为物品施加镀层配方 |
| 反弹 | `stream/cap/EtherStreamBounceBackCapability.java` | 碰撞反向弹回 |
| 成本缩减 | `stream/cap/EtherStreamCostReducerCapability.java` | 降低以太消耗 |
| 物品显示 | `stream/cap/EtherStreamItemDisplayCapability.java` | 显示携带物品图标 |

**IStreamCapability 接口**:
```java
interface IStreamCapability extends ValueIOSerializable {
    Identifier getId();
    void getConsumption(EtherConsumer, IEtherStreamLike); // 计算消耗
    void setConsumer(EtherConsumer);
    void tick(IEtherStreamLike);                          // 每tick
    boolean hitEntity(ServerLevel, IEtherStreamLike, EntityHitResult, Entity);
    boolean hitBlock(ServerLevel, IEtherStreamLike, BlockHitResult, BlockState);
    boolean onBeforeDestroy(IEtherStreamLike, HitResult); // 销毁前
    void onDestroy(IEtherStreamLike, HitResult);          // 销毁时
    void firstTick(IEtherStreamLike);                     // 首次tick
    boolean shouldPassThrough(BlockState/Entity);         // 穿透判断
    void onRecreate(IEtherStreamLike);                    // 重建时
}
```

### 4.4 虚拟以太流管理

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 虚拟管理器 | `stream/vholder/VirtualEtherStreamHolderManager.java` | 防止同位置重复发射, 管理活跃流 |
| 附件存储 | `AttachmentDataRegistry.java` | VESHM 附件 |

### 4.5 客户端渲染

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 渲染事件 | `event/EtherStreamRenderEvent.java` | 以太流粒子渲染 |
| 客户端管理 | `stream/client/EntityStreamClientManager.java` | 客户端流状态管理 |
| 同步数据 | `stream/data/IEtherStreamSyncedData.java` | 同步数据接口 |
| 携带实体数据 | `stream/data/EtherStreamCarryingEntityData.java` | 携带实体同步 |
| 显示物品数据 | `stream/data/EtherStreamDisplayItemData.java` | 显示物品同步 |
| 数据管理器 | `stream/data/SyncedEtherStreamDataManager.java` | 同步数据序列化 |

---

## 五、镀层系统 (Plating)

### 5.1 核心系统

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 镀层管理器 | `plating/PlatingManager.java` | 镀层效果注册 |
| 镀层工具 | `plating/helper/PlatingUtil.java` | 镀层数据读写: hasPlating/getEther/extractEther/addEther |
| 充能工具 | `plating/helper/PlatingChargingUtil.java` | 以太流为镀层物品充能 |
| 进度处理 | `plating/event/PlatingProgressHandler.java` | 镀层进行中的 tick 处理 |
| 效果接口 | `plating/effects/IPlatingEffect.java` | 镀层效果接口 |
| 镀层配方 | `recipe/plating/PlatingRecipe.java` | 镀层配方: 输入物品+效果+公式+过滤器 |
| 数据组件 | `DataComponentRegistry.java` | PLATING_DATA, PLATING_ETHER, PLATING_IN_PROGRESS, PLATING_START_TIME |

**镀层数据模型**:
```
PlatingData: (id, effect, coolDownUntil)
  - id: 效果标识
  - effect: 效果值(double)
  - coolDownUntil: 冷却结束时间(可选)

ProgressingPlatingData: (id, formula)
  - id: 效果标识
  - formula: PlatingEffectFormula(a1, a2, a3, a4)

PlatingEffectFormula: (a1, a2, a3, a4)
  - getEffect(e): clamp(e, a1, a2) 线性映射到 a3~a4
```

**镀层流程**:
```
1. 以太流携带材料 + 目标物品碰撞
   -> EtherStreamPlatingCapability.tick()
   -> 匹配 PlatingRecipe (精确覆盖匹配算法)
   -> PlatingUtil.startPlating(stack, effectIds, gameTime)

2. 镀层进行中 (PlatingProgressHandler.tick):
   -> 每 tick 显示蓝色粒子
   -> elapsed >= platingDurationTicks?
   -> PlatingUtil.updatePlatingData(stack, platingData)

3. 充能 (PlatingChargingUtil):
   -> 以太流碰撞镀层物品/盔甲架/书架/玩家
   -> distributeCharge(): 平均分配以太到所有镀层装备

4. 效果触发:
   -> 各效果通过事件监听触发
   -> 检查 PlatingUtil.canExtractEther(stack, cost)
   -> 消耗以太 -> 执行效果
   -> 以太耗尽 -> PlatingUtil.clearPlating(stack)
```

### 5.2 镀层效果实现

**目录**: `plating/effects/`

| 效果 | 文件 | 触发方式 | 实现接口 |
|------|------|---------|---------|
| 伤害 | DamagePlatingEffect.java | AttackEntityEvent | IPlatingAttackTrigger |
| 冲刺 | DashPlatingEffect.java | RightClickItem / Key | IPlatingRightClickTrigger + IPlatingKeyTrigger |
| 高跳 | HighJumpPlatingEffect.java | RightClickItem / Key | IPlatingRightClickTrigger + IPlatingKeyTrigger |
| 无重力 | NoGravityPlatingEffect.java | 射箭 | IPlatingArrowTrigger |
| 郊狼时间 | CoyoteTimePlatingEffect.java | 跳跃 | IPlatingTickEquippedTrigger |
| 伪装 | CamouflagePlatingEffect.java | PlayerTick | IPlatingTickEquippedTrigger + IEffectStartAndEndTrigger |
| 格挡 | BlockPlatingEffect.java | RightClickItem | IPlatingRightClickTrigger |
| 暴击 | CritPlatingEffect.java | AttackEntityEvent | IPlatingAttackTrigger |
| 暴击伤害 | CritDamagePlatingEffect.java | 暴击时 | IPlatingCritTrigger |
| 猎头 | HeadHuntPlatingEffect.java | LivingDeathEvent | IPlatingKillTrigger |
| 追踪 | TrackingPlatingEffect.java | 射箭 | IPlatingArrowTrigger |
| 破坏进包 | BreakToInventoryPlatingEffect.java | BlockEvent.BreakEvent | IPlatingBreakTrigger |
| 击杀进包 | KillToInventoryPlatingEffect.java | LivingDeathEvent | IPlatingKillTrigger |
| 吃废石 | StoneAbsorbPlatingEffect.java | BlockEvent.BreakEvent | IPlatingBreakTrigger |
| 以太穿行 | EtherStreamDashPlatingEffect.java | RightClickItem | IPlatingRightClickTrigger |
| 以太穿行(快) | EtherStreamDashFasterPlatingEffect.java | RightClickItem | extends EtherStreamDashPlatingEffect |
| 以太伤害 | EtherStreamDamagePlatingEffect.java | RightClickItem | IPlatingRightClickTrigger |
| 以太破坏 | EtherStreamBreakPlatingEffect.java | RightClickItem | IPlatingRightClickTrigger |
| 抗黑暗 | AntiDarknessPlatingEffect.java | 被动 | IPlatingTickEquippedTrigger |
| 伦理 | EthicPlatingEffect.java | AttackEntityEvent | IPlatingAttackTrigger |
| 抗音爆 | AntiSonicBoomPlatingEffect.java | 被动 | IPlatingDamageTrigger |
| 静步 | SilentStepPlatingEffect.java | 被动 | IPlatingVibrationTrigger |
| 耐久吸收 | DurabilityAbsorptionPlatingEffect.java | 被动 | IPlatingDurabilityTrigger |

### 5.3 触发系统

**目录**: `plating/trigger/`

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 事件触发 | `plating/trigger/event/` | 各种事件触发器接口 |
| 实例触发 | `plating/trigger/inst/` | 镀层安装/卸载时的回调 |
| 触发管理 | `plating/event/` | 事件监听器, 分发到对应效果 |

**触发器接口**:
```
IPlatingAttackTrigger    -> AttackEntityEvent
IPlatingRightClickTrigger -> PlayerInteractEvent.RightClickItem
IPlatingKeyTrigger       -> 按键触发
IPlatingTickEquippedTrigger -> PlayerTickEvent.Post
IPlatingArrowTrigger     -> 射箭事件
IPlatingKillTrigger      -> LivingDeathEvent
IPlatingBreakTrigger     -> BlockEvent.BreakEvent
IPlatingCritTrigger      -> 暴击事件
IPlatingDamageTrigger    -> 受伤事件
IPlatingVibrationTrigger -> 振动事件
IPlatingDurabilityTrigger -> 耐久消耗事件
IEffectStartAndEndTrigger -> 效果安装/卸载
```

---

## 六、配方系统总览

### 6.1 配方类型

**子部分**:

| 配方类型 | 注册 | 序列化器 | 用途 |
|---------|------|---------|------|
| ether_process | RecipeTypeRegistry.ETHER_PROCESS_FACTORY_RECIPE | ETHER_PROCESS_RECIPE_SERIALIZER | 工厂加工配方 |
| node_process | RecipeTypeRegistry.NODE_PROCESS_RECIPE | NODE_PROCESS_RECIPE_SERIALIZER | 节点加工配方 |
| ether_process_factory_grid | RecipeTypeRegistry.ETHER_PROCESS_FACTORY_GRID | ETHER_PROCESS_FACTORY_GRID_SERIALIZER | 工厂网格配方 |
| plating | RecipeTypeRegistry.PLATING_RECIPE | PLATING_RECIPE_SERIALIZER | 镀层配方 |
| upgrade_shaped | 无独立类型 | UPGRADE_SHAPED_RECIPE_SERIALIZER | 升级合成(保留组件) |

### 6.2 数据目录

| 目录 | 内容 |
|------|------|
| `data/ether_craft/recipe/crafting/` | 工作台合成配方 |
| `data/ether_craft/recipe/ether_process/` | 工厂加工配方 |
| `data/ether_craft/recipe/ether_adapt/` | 节点加工配方 |
| `data/ether_craft/recipe/grid/` | 工厂网格配方 |
| `data/ether_craft/recipe/plating/` | 镀层配方 |
| `data/ether_craft/ether_process_chip/` | 芯片数据定义 |

---

## 七、网络系统

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 网络主类 | `network/Network.java` | 协议版本, 服务端/客户端注册器 |
| 客户端网络 | `network/NetworkClient.java` | 客户端包处理器 |
| C2S 包 | `network/c2s/` | 客户端->服务端: GUI 交互、屏幕数据同步 |
| S2C 包 | `network/s2c/` | 服务端->客户端: 以太值同步、名称同步、插件数据同步 |
| 基础包 | `network/base/` | 包基类 |

---

## 八、事件系统

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 服务端启动 | `event/ServerStartUpEvent.java` | 服务端初始化 |
| 客户端启动 | `event/ClientStartupEvent.java` | 客户端初始化, 渲染器收集 |
| 服务端Tick | `event/ServerTickHandler.java` | 服务端 tick 调度 |
| 客户端Tick | `event/ClientTickEvent.java` | 客户端 tick |
| 物品实体Tick | `event/ItemEntityTickEvent.java` | 以太失活转化, 镀层进度 |
| 配方同步 | `event/ServerRecipeSyncEvent.java` / `ClientRecipeSyncEvent.java` | 配方网络同步 |
| 以太流同步 | `event/EtherStreamSyncHandler.java` | 以太流数据同步 |
| 以太流渲染 | `event/EtherStreamRenderEvent.java` | 以太流粒子渲染 |
| 世界渲染 | `event/WorldRenderEvent.java` | 世界渲染钩子 |
| 玩家渲染 | `event/PlayerRenderEvent.java` | 玩家渲染钩子 |
| 监守者掉落 | `event/WardenDropHandler.java` | 监守者之心掉落 |
| 调试 | `event/DebugAddEvent.java` | 调试工具 |

---

## 九、注册系统

**子部分**:

| 注册表 | 文件 | 注册内容 |
|--------|------|---------|
| 方块 | `BlockRegistry.java` | 7个方块 |
| 物品 | `ItemRegistry.java` | 20+物品 |
| 方块实体 | `BlockEntityRegistry.java` | 3个实体类型 |
| 实体 | `EntityRegistry.java` | 以太流实体 |
| 配方类型 | `RecipeTypeRegistry.java` | 4个配方类型 |
| 配方序列化器 | `RecipeSerializerRegistry.java` | 5个序列化器 |
| 创造标签页 | `CreativeTabRegistry.java` | 1个标签页 |
| 数据组件 | `DataComponentRegistry.java` | 9个数据组件 |
| 能力 | `CapabilityRegistry.java` | 以太容器 + 物品能力 |
| GUI | `GuiRegistry.java` + `ClientGuiRegistry.java` | 菜单屏幕 |
| 附件 | `AttachmentDataRegistry.java` | 8个附件类型 |
| 实体数据序列化器 | `EntityDataSerializerRegistry.java` | 自定义序列化器 |
| 数据加载 | `DataLoadRegister.java` | 资源重载监听 |
| 数据映射 | `DataMapRegister.java` | 数据映射注册 |
| 渲染模型 | `RenderModelRegister.java` | 模型注册 |
| 特殊渲染器 | `SpecialRendererRegister.java` | 特殊渲染器注册 |
| 标签 | `Tags.java` | 9个标签键 |

---

## 十、集成系统

**子部分**:

| 集成 | 文件 | 职责 |
|------|------|------|
| JEI | `integration/jei/` | JEI 配方查看: 工厂配方、节点插件信息、镀层配方 |
| Jade | `integration/jade/` | Jade 信息显示: 节点等级、以太量、插件数 |
| Iris | `integration/iris/` | Iris 着色器兼容 |
| Sodium | `integration/sodium/` | Sodium 渲染兼容 |
| 集成管理 | `integration/Integrations.java` | 模组加载检测 |

---

## 十一、数据生成

**子部分**:

| 子模块 | 文件 | 职责 |
|--------|------|------|
| 方块标签 | `datagen/TagGenBlock.java` | 方块标签生成 |
| 物品标签 | `datagen/TagGenItem.java` | 物品标签生成 |
| 实体标签 | `datagen/TagGenEntity.java` | 实体标签生成 |
| 模型 | `datagen/ModelDataGen.java` | 模型生成 |
| 数据映射 | `datagen/DataMapGen.java` | 数据映射生成 |
| 世界生成 | `datagen/WorldGenData.java` | 矿脉生成数据 |
| 战利品表 | `datagen/LootTableGen.java` | 战利品表生成 |
| 收集事件 | `datagen/GenerateGatherEvent.java` | 数据生成收集 |

---

## 十二、工具类

**子部分**:

| 工具 | 文件 | 职责 |
|------|------|------|
| 集合工具 | `util/CollectionUtil.java` | 全排列索引生成 |
| 容器操作 | `util/ContainerOps.java` | 容器序列化/传输 |
| 配方工具 | `util/EtherProcessorRecipeUtil.java` | 工厂配方匹配算法 |
| 合并集合 | `util/MergeSet.java` | 合并集合 |
| 序列化工具 | `util/SerializeUtil.java` | 序列化辅助 |
| 集合工具 | `util/SetUtil.java` | 二分图匹配 |
| UI工具 | `util/UIUtil.java` | GUI 渲染辅助 |
| 图结构 | `base/GraphLike.java` | 通用图结构 |
| 树结构 | `base/TreeLike.java` | 通用树结构 |

---

## 十三、配置系统

**文件**: `Config.java`

**配置分类**:

| 分类 | 配置项数 | 说明 |
|------|---------|------|
| ether | 2 | 以太转化比例、失活转化时间 |
| node | 20+ | 节点默认以太、升级槽位、各插件参数 |
| ether_stream | 8 | 流最大寿命、消耗因子、破坏/伤害参数 |
| plating | 30+ | 镀层持续时间、各效果消耗/冷却 |
