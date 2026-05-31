# 三个 EAN 插件升级 — 设计文档

> 2026-05-31 | ether_craft NeoForge 1.26.1.2

## 概述

为 `EtherAdaptNode` 添加三个新的插件升级，分别使用比较器、红石粉、熔岩桶作为安装物品。

---

## 1. FeatureRedstoneSignal（红石信号升级）

**物品：** `Items.COMPARATOR`（比较器）
**类型：** `FEATURE`（方向性特性）
**继承：** `AbstractDirectionalFeature`

### 功能

- 占用 EAN 的一个面（通过 `Direction direction` 字段，与其他方向性 feature 互斥）
- 根据当前机器的以太存量或物品仓库存量向外发出红石比较器信号
- 信号范围为 0-15，实时计算

### 字段

```java
enum SignalMode { ETHER, INVENTORY }

SignalMode mode = SignalMode.ETHER;   // 信号来源模式
boolean enabled = true;               // 开关
```

### 信号计算

| 模式 | 公式 |
|------|------|
| ETHER | `floor(currentEther / maxEther * 15)` |
| INVENTORY | `floor(已填充槽位数 / 已解锁槽位数 * 15)` |

### 红石输出集成

- `EtherAdaptNodeBlock` 重写 `hasAnalogOutputSignal(BlockState)` — 当存在已安装且启用的 `FeatureRedstoneSignal` 时返回 `true`
- `EtherAdaptNodeBlock` 重写 `getAnalogOutputSignal(BlockState, Level, BlockPos)` — 委托给 `EtherAdaptNodeEntity.getAnalogOutputSignal()`
- `EtherAdaptNodeEntity.getAnalogOutputSignal()` — 遍历 upgrade 插件容器，找到 `FeatureRedstoneSignal` 实例并计算信号值；未找到则返回 0

### GUI

- 模式选择器：ETHER / INVENTORY
- 启用开关：ON / OFF

### 序列化

- `mode` — 通过 `EnumCodec` 存储
- `enabled` — 通过 `Codec.BOOL` 存储
- 继承 `AbstractDirectionalFeature` 已有的 `direction` 序列化

---

## 2. RedstoneSwitchUpgrade（红石开关升级）

**物品：** `Items.REDSTONE`（红石粉）
**类型：** `UPGRADE`
**继承：** `AbstractNodePlugin`（直接）

### 功能

- 持有一个开关，配置 EAN 在有/无红石信号时才工作
- 通过 `preTick()` 机制作为全局门控：任一 switch 插件返回 false，整个节点跳过本次 tick

### 字段

```java
boolean workWithSignal = true;  // true=有红石信号时工作, false=无红石信号时工作
```

### preTick 机制

**AbstractNodePlugin 新增方法：**

```java
public boolean preTick() {
    return true;  // 默认：允许工作
}
```

**EtherAdaptNodeEntity.tickServer() 门控逻辑：**

```java
void tickServer() {
    // 新增：preTick 门控
    if (!functionStorage.preTick()) return;
    if (!featureUpgradeStorage.preTick()) return;
    // ... 原有 tick 流程 ...
}
```

**EtherPluginUpgradeContainer.preTick()：**

```java
boolean preTick() {
    for (AbstractNodePlugin plugin : this.plugin) {
        if (plugin != null && !plugin.preTick())
            return false;
    }
    return true;
}
```

**RedstoneSwitchUpgrade.preTick()：**

```java
public boolean preTick() {
    boolean hasSignal = nodeEntity.getLevel().hasNeighborSignal(nodeEntity.getBlockPos());
    return workWithSignal ? hasSignal : !hasSignal;
}
```

### GUI

- 模式切换按钮：workWithSignal（有信号工作 / 无信号工作）

### 序列化

- `workWithSignal` — 通过 `Codec.BOOL` 存储

---

## 3. DestructionUpgrade（销毁升级）

**物品：** `Items.LAVA_BUCKET`（熔岩桶）
**类型：** `UPGRADE`
**继承：** `AbstractNodePlugin`（直接）

### 功能

- 持有过滤器（默认白名单），有一个开关控制销毁模式
- ALL 模式：匹配 items 直接销毁，不进入库存
- OVERFLOW 模式：正常插入库存后，销毁溢出的匹配 items

### 字段

```java
enum DestroyMode { OVERFLOW, ALL }

ItemFilter filter = new ItemFilter(21, nodeEntity::setChanged);
DestroyMode destroyMode = DestroyMode.OVERFLOW;  // 默认溢出销毁
```

### 生命周期方法

| 模式 | 重写方法 | 行为 |
|------|---------|------|
| ALL | `earlyHandleInput()` | 若 filter 匹配，返回 `amount`，全部销毁 |
| OVERFLOW | `handleOverflow()` | 正常插入完成后，若 filter 匹配，销毁溢出差额 |

### handleOverflow 机制

**AbstractNodePlugin 新增方法：**

```java
public int handleOverflow(ItemResource resource, int amount, TransactionContext transaction) {
    return 0;  // 默认：不处理溢出
}
```

**EtherAdaptNodeEntity.insert() 流程：**

```java
insert(index, resource, amount, transaction):
  // 1. 现有检查（slot index, filter, etc.）
  // 2. earlyHandleInput（现有逻辑）
  int earlyCosted = 0;
  for (plugin : getPlugins())
      earlyCosted += plugin.earlyHandleInput(resource, amount - earlyCosted, transaction);
  if (earlyCosted >= amount) return earlyCosted;
  
  // 3. 正常插入库存
  int handlerInserted = normalHandler.insert(index-1, resource, amount - earlyCosted, transaction);
  
  // 4. handleOverflow（新增）
  int overflow = amount - earlyCosted - handlerInserted;
  int overflowConsumed = 0;
  for (plugin : getPlugins())
      overflowConsumed += plugin.handleOverflow(resource, overflow - overflowConsumed, transaction);
  
  return handlerInserted + earlyCosted + overflowConsumed;
```

**EtherPluginUpgradeContainer 新增委托方法：**

```java
int handleOverflow(ItemResource resource, int amount, TransactionContext transaction) {
    int consumed = 0;
    for (AbstractNodePlugin plugin : this.plugin) {
        if (plugin != null)
            consumed += plugin.handleOverflow(resource, amount - consumed, transaction);
    }
    return consumed;
}
```

**DestructionUpgrade.handleOverflow()（OVERFLOW 模式）：**

```java
public int handleOverflow(ItemResource resource, int amount, TransactionContext transaction) {
    if (destroyMode != DestroyMode.OVERFLOW) return 0;
    if (!filter.accepts(resource)) return 0;
    return amount;  // 销毁所有溢出量
}
```

**DestructionUpgrade.earlyHandleInput()（ALL 模式）：**

```java
public int earlyHandleInput(ItemResource resource, int amount, TransactionContext transaction) {
    if (destroyMode != DestroyMode.ALL) return 0;
    if (!filter.accepts(resource)) return 0;
    return amount;  // 全部销毁
}
```

### GUI

- 过滤器网格：21 格（与现有 `AbstractDirectionalFilterFeature` 一致，使用 `FilterGuiRegCommon.slots()`）
- 模式切换按钮：OVERFLOW / ALL

### 序列化

- `filter` — 通过 `ItemFilter` 的 `ValueIOSerializable` 接口存储
- `destroyMode` — 通过 `EnumCodec` 存储

---

## 修改汇总

### 修改的现有文件

| 文件 | 变更内容 |
|------|---------|
| `AbstractNodePlugin.java` | 新增 `preTick()` 默认方法（返回 `true`）；新增 `handleOverflow(...)` 默认方法（返回 `0`） |
| `EtherPluginUpgradeContainer.java` | 新增 `preTick()` 遍历委托方法；新增 `handleOverflow(...)` 遍历委托方法 |
| `EtherAdaptNodeEntity.java` | `tickServer()` 开头新增 preTick 门控检查；`insert()` 末尾新增 handleOverflow 调用；新增 `getAnalogOutputSignal()` 方法 |
| `EtherAdaptNodeBlock.java` | 重写 `hasAnalogOutputSignal(BlockState)`；重写 `getAnalogOutputSignal(BlockState, Level, BlockPos)` |
| `NodePluginManager.java` | 注册 3 个新 PluginInfo |

### 新建文件

| 文件 | 说明 |
|------|------|
| `node/plugins/feature/FeatureRedstoneSignal.java` | 红石信号比较器 feature 插件 |
| `node/plugins/upgrade/RedstoneSwitchUpgrade.java` | 红石开关 upgrade 插件 |
| `node/plugins/upgrade/DestructionUpgrade.java` | 销毁 upgrade 插件 |

---

## 插入流程总览（修改后）

```
EtherAdaptNodeEntity.insert(resource, amount, transaction):

  ┌─────────────────────────────────────────┐
  │ 1. 基本检查（index, is(ETHER), slotUnlock, filter）│
  ├─────────────────────────────────────────┤
  │ 2. earlyHandleInput（全部插件遍历）         │
  │    函数插件：ether converter 等消耗          │
  │    升级插件：DestructionUpgrade(ALL 模式)    │
  │    → earlyCosted                         │
  ├─────────────────────────────────────────┤
  │ 3. normalHandler.insert() 正常插入库存      │
  │    → handlerInserted                     │
  ├─────────────────────────────────────────┤
  │ 4. handleOverflow（新增，全部插件遍历）       │
  │    升级插件：DestructionUpgrade(OVERFLOW 模式)│
  │    → overflowConsumed                    │
  ├─────────────────────────────────────────┤
  │ return handlerInserted + earlyCosted + overflowConsumed │
  └─────────────────────────────────────────┘
```

## Tick 流程总览（修改后）

```
EtherAdaptNodeEntity.tickServer():

  ┌─────────────────────────────────────────┐
  │ 新增：preTick 门控                        │
  │ functionStorage.preTick() → 全部函数插件    │
  │ featureUpgradeStorage.preTick() → 全部升级插件│
  │ 任一返回 false → return（跳过本次 tick）     │
  ├─────────────────────────────────────────┤
  │ 原有：tickInput / tickWork / tickOutput   │
  │ 原有：ticket.tick()                       │
  │ 原有：markUpdate → updateProperty()      │
  └─────────────────────────────────────────┘
```
