# 设计：冒险模式禁止扳手操作

日期：2026-08-10

## 背景

当前模组中扳手的各种世界内交互直接调用 `destroyBlock` 或 `setBlockAndUpdate`，绕过了冒险模式（Adventure Mode）原版的放置/破坏限制。玩家在冒险模式下仍可拆除方块、旋转机器、切换玻璃 cull 面、安装插件。

## 目标

在冒险模式下，禁止以下扳手世界内操作，全部静默无效（不发送任何提示消息）：

1. 蹲下 + 右键拆除方块（`ItemWrench.useOn`）
2. 右键旋转以太适配节点朝向及特性插件方向（`EtherAdaptNodeBlock.useItemOn`）
3. 右键旋转以太加工工厂朝向（`EtherProcessFactoryBlock.useItemOn`）
4. 右键切换以太玻璃 cull 面（`EtherCullGlassBlock.useItemOn`）
5. 副手持扳手 + 主手持插件快速安装插件（`EtherAdaptNodeBlock.useItemOn`）

## 检测方式

使用 `!player.mayBuild()` 判断冒险模式。

依据：`GameType.updatePlayerAbilities` 中 `abilities.mayBuild = !isBlockPlacingRestricted()`，冒险模式与旁观模式均为 `false`。旁观模式玩家已被原版 `ServerPlayerGameMode.useItemOn` 拦截，无法触发本模组交互，故无副作用。

该判断在客户端（`LocalPlayer.mayBuild()` 同步了服务端 abilities）与服务端行为一致，可确保客户端也不触发交互反馈。

## 修改点

所有改动均在现有交互分支内内联判断，无新增文件、无配置项、无语言键。被拒时返回 `InteractionResult.PASS`。

### 1. `src/main/java/studio/fantasyit/ether_craft/item/ItemWrench.java`

在 `useOn` 中 `player == null` 检查之后、shift 判断之前加入：

```java
if (!player.mayBuild())
    return InteractionResult.PASS;
```

阻止蹲下拆除方块。

### 2. `src/main/java/studio/fantasyit/ether_craft/block/node/EtherAdaptNodeBlock.java`

在 `useItemOn` 方法开头加入统一守卫，同时覆盖旋转节点与副手扳手快速安装两个入口：

```java
if (!player.mayBuild() && (itemStack.is(ItemRegistry.WRENCH) || player.getOffhandItem().is(ItemRegistry.WRENCH)))
    return InteractionResult.PASS;
```

返回 `PASS` 后，主手持插件/空手的情况不会触发误打开 GUI（`useWithoutItem` 仅在 `TryEmptyHandInteraction` 时触发）。

### 3. `src/main/java/studio/fantasyit/ether_craft/block/factory/EtherProcessFactoryBlock.java`

在 `useItemOn` 的 `if (itemStack.is(ItemRegistry.WRENCH))` 分支内加入：

```java
if (!player.mayBuild())
    return InteractionResult.PASS;
```

阻止旋转工厂朝向。

### 4. `src/main/java/studio/fantasyit/ether_craft/block/glass/EtherCullGlassBlock.java`

在 `useItemOn` 的 `if (itemStack.is(ItemRegistry.WRENCH) && !player.isShiftKeyDown())` 分支内加入：

```java
if (!player.mayBuild())
    return InteractionResult.PASS;
```

阻止切换玻璃 cull 面。

## 不受影响的行为

- GUI 内扳手快捷操作（快速放置/克隆芯片）——只移动物品，不修改世界
- 空手右键打开机器 GUI
- 生存/创造模式下的全部扳手功能

## 验证

使用 `idea_build_project` 编译验证，无测试套件需求。
