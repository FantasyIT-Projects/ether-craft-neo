# 以太流同步状态指示器调试层 — 设计文档

## 概述

为以太流的**虚拟流**同步链路添加一个纯客户端调试层：每当客户端收到添加/更新/删除某个以太流的 S2C 同步包时，在该流当前位置记录一个标记，渲染为原版 `Gizmos`（彩色方框 + 文字标签），1 秒（20 tick）后消失。

目的：直观观察虚拟流的同步行为，便于调试同步问题。

## 范围

- **仅覆盖虚拟流**（`VirtualEtherStream` 经 S2C 包同步的链路），不覆盖实体流（`EtherStreamEntity`）。
- **纯客户端**：事件在 `ClientVESHData` 的 S2C handler 中本地记录，无网络改动、无服务端参与。
- **渲染**：使用原版 `DebugRenderer.SimpleDebugRenderer` + `Gizmos` 机制。

## 技术背景（已核实，26.1.2）

- `DebugRenderer.emitGizmos` 在 `Gizmos.withCollector(...)` 帧上下文中被调用（`Minecraft.java:1321` → `renderFrame` → `extractLevel` → `LevelRenderer.java:615`），因此 `SimpleDebugRenderer.emitGizmos` 内可直接调用 `Gizmos.cuboid(...)`、`Gizmos.billboardText(...)`。
- 自定义 `SimpleDebugRenderer` 通过 NeoForge 的 `RegisterDebugRenderersEvent`（mod 总线）注册，注册方式为 `Function<Minecraft, SimpleDebugRenderer>` 工厂。
- `GizmoStyle.stroke(argb)`、`TextGizmo.Style.forColorAndCentered(argb)`、`Gizmos.cuboid(aabb, style)`、`Gizmos.billboardText(text, pos, style).setAlwaysOnTop()` 均为可用的公开 API。
- 客户端为单线程模型：包处理（`ctx.enqueueWork`）、`ClientTickEvent`、`emitGizmos` 均发生在主线程。开关布尔 **不需要 `volatile`**。

## 组件

### 1. `client/debug/EtherStreamSyncMarker.java`（新建）

静态事件存储与生命周期管理：

- `public enum Type { CREATE, UPDATE, DELETE }`
- `record Marker(Type type, Vec3 pos, int streamId, long gameTime)`
- `public static boolean ENABLED = false` —— 普通 `static boolean`，主线程读写，无需 `volatile`。
- `private static final int LIFETIME_TICKS = 20;`
- `private static final List<Marker> markers = new ArrayList<>();`
- `public static void record(Type type, Vec3 pos, int streamId)`：
  - 第一行 `if (!ENABLED) return;`（关闭时零分配零开销）
  - 追加 `new Marker(type, pos, streamId, Minecraft.getInstance().level.getGameTime())`
- `public static void tick()`：`if (markers.isEmpty()) return;`，清理 `gameTime - marker.gameTime > LIFETIME_TICKS` 的条目。
- `public static List<Marker> getMarkers()`：返回当前存活标记（供渲染器遍历）。
- `public static boolean isEnabled()` / `setEnabled(boolean)`。

**无上限**：不做数量上限保护，靠 20 tick 自动过期回收。

### 2. `client/debug/EtherStreamSyncDebugRenderer.java`（新建）

实现 `DebugRenderer.SimpleDebugRenderer`：

- `emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks)`：
  - 第一行 `if (!EtherStreamSyncMarker.isEnabled()) return;`（关闭时零开销）
  - 遍历 `EtherStreamSyncMarker.getMarkers()`，计算剩余寿命比例 `progress = 1 - age / LIFETIME_TICKS`（`age` 由当前游戏刻与 `marker.gameTime` 差值得到，可叠加 partialTick）。
  - 按 `progress` 计算方框大小（初始 `0.12f` 缩小到接近 0）与颜色 alpha（淡出）。
  - 三种颜色：
    - CREATE：绿色 `0xFF00FF00`
    - UPDATE：黄色 `0xFFFFFF00`
    - DELETE：红色 `0xFFFF0000`
  - 方框：`Gizmos.cuboid(new AABB(pos).inflate(scale), GizmoStyle.stroke(color, width))`
  - 文字：`Gizmos.billboardText("C" + streamId / "U" + streamId / "D" + streamId, pos, TextGizmo.Style.forColorAndCentered(color)).setAlwaysOnTop()`
- 参照原版 `NeighborsUpdateRenderer` 的 shrink + 计数文字风格。

### 3. `client/key/EtherStreamSyncDebugKeyHandler.java`（新建）

- `public static final KeyMapping SYNC_MARKER = new KeyMapping("key.ether_craft.sync_marker", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), PlatingTriggerKeyHandler.ETHER_CRAFT_CATEGORY);`
  - **默认按键留空**（`InputConstants.UNKNOWN`）。F3+S 组合键无法仅靠 `KeyMapping` 表达，故不设默认键，由玩家自行绑定。
- `@SubscribeEvent registerKeyMappings(RegisterKeyMappingsEvent)` 注册。
- `@SubscribeEvent onClientTick(ClientTickEvent.Post)`：`while (SYNC_MARKER.consumeClick()) EtherStreamSyncMarker.setEnabled(!EtherStreamSyncMarker.isEnabled());`
- 语言键 `key.ether_craft.sync_marker` 添加到 en_us.json / zh_cn.json。

### 4. `ClientVESHData.java`（修改）

在各 S2C handler 中接入 `record(...)`（位置取对应 `ClientStreamEntry` 的位置）：

- `handleCreate(EtherStreamInitialCreateS2C)`：新流 `addStream` 后 `record(CREATE, entry.currentPos, streamId)`。
- `handleCreate(EtherStreamBatchCreateS2C)`：每个新 entry `record(CREATE, ...)`。
- `handleQuickCreate(EtherStreamQuickCreateS2C)`：`addStream` 后 `record(CREATE, quickEntry 推算位置, streamId)`（位置为推算结果，可接受）。
- `handleUpdate(EtherStreamUpdateS2C)`：对每个存在的 entry `record(UPDATE, current.currentPos, streamId)`。
- `handleSync(EtherStreamSyncDataS2C)`：`record(UPDATE, ...)`。
- `handleDying(EtherStreamSetDyingS2C)`：对每个存在的 entry `record(DELETE, current.currentPos, streamId)`。

### 5. 渲染器注册

- 通过 `RegisterDebugRenderersEvent` 注册 `EtherStreamSyncDebugRenderer`（`register(mc -> new EtherStreamSyncDebugRenderer())`）。
- 注册位置：新建或复用一个 `@EventBusSubscriber(modid = ..., value = Dist.CLIENT)` 客户端事件订阅类（mod 总线事件）。可放在 `EtherStreamSyncDebugRenderer` 自身作为内部静态订阅，或独立事件类。

### 6. `tick()` 驱动

- mod 已有客户端 tick 入口 `event/ClientTickEvent.onLevelTick`（`LevelTickEvent.Post`），每 tick 调用 `ClientVESHData.getWithCurrentLevel(mc.level).tick()`。
- 在该方法中追加 `EtherStreamSyncMarker.tick();`，无需新增订阅。`gameTime` 时间基准与标记记录一致（同一 level）。

## 零开销保证（关闭状态）

| 位置 | 关闭时行为 | 残余开销 |
|---|---|---|
| `record()` | 首行 `if (!ENABLED) return;` | 1 次布尔读取，零分配 |
| `emitGizmos()` | 首行 `if (!isEnabled()) return;` | 每帧 1 次布尔读取，不创建 Gizmo |
| 标记存储 | 始终为空 `ArrayList` | 0 元素 |
| `tick()` | 空列表短路 | 空列表判断 |
| 按键轮询 | `consumeClick()` 每 tick | 单键读取（开关前提，无法避免） |

## 关键决策

| 项 | 决定 |
|---|---|
| 覆盖范围 | 仅虚拟流（VESH） |
| 事件来源 | 纯客户端，收到 S2C 包时记录 |
| 位置 | 事件发生时流的位置，固定不跟随 |
| 区分方式 | 颜色（绿/黄/红）+ 文字标签（C/U/D + streamId） |
| 生命周期 | 20 tick（1 秒），shrink + 淡出 |
| 数量上限 | 无上限，靠 20 tick 自动过期回收 |
| 开关 | `KeyMapping`，默认按键留空 |
| 并发 | 普通 `static boolean`，无 `volatile`（单线程） |

## 错误处理

- `handleQuickCreate` 推算位置失败或 `entry == null` 时跳过记录（不崩溃）。
- `handleDying`/`handleUpdate` 中 entry 不存在或已 `removed`/`isDying` 时按现有逻辑跳过。
- 渲染器对已过期标记不做特殊处理——`tick()` 保证它们被移除。

## 测试

- 编译通过（`idea_build_project`）。
- 运行客户端：打开调试层开关（手动绑定按键），放置 Ether Stream Emitter，观察添加/更新/删除标记的颜色、位置、1 秒后消失。
- 关闭开关：确认无标记记录、渲染器每帧立即返回、无性能可感知差异。
