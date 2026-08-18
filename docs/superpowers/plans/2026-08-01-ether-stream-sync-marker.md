# 以太流同步状态指示器调试层 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为虚拟以太流的 S2C 同步链路添加纯客户端调试层：收到添加/更新/删除同步包时，在流当前位置记录一个彩色 `Gizmos` 标记（绿/黄/红 + C/U/D 文字标签），1 秒（20 tick）后 shrink 淡出消失；按键开关默认留空；关闭时零开销。

**Architecture:** 纯客户端。`EtherStreamSyncMarker` 持有静态事件列表与 `ENABLED` 开关；`EtherStreamSyncDebugRenderer` 实现原版 `DebugRenderer.SimpleDebugRenderer`，在 `emitGizmos` 首行短路后遍历标记并调用 `Gizmos.cuboid` / `Gizmos.billboardText`；`EtherStreamSyncDebugKeyHandler` 用 `KeyMapping` 切换开关；`ClientVESHData` 五个 handler 接入 `record(...)`；mod 已有 `event/ClientTickEvent.onLevelTick` 驱动 `tick()` 清理。

**Tech Stack:** NeoForge 26.1.2.61-beta, Minecraft 26.1.2, Java 25。渲染使用原版 `DebugRenderer.SimpleDebugRenderer` + `net.minecraft.gizmos.Gizmos`（已验证 `emitGizmos` 运行于 `Gizmos.withCollector` 帧上下文内）。

---

### Task 1: 创建 `EtherStreamSyncMarker` 静态存储

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/client/debug/EtherStreamSyncMarker.java`

无独立测试框架（纯视觉调试功能），验证方式为本 mod 的标准 `idea_build_project` 编译。

- [ ] **Step 1: 创建文件**

```java
package studio.fantasyit.ether_craft.client.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class EtherStreamSyncMarker {
    public enum Type { CREATE, UPDATE, DELETE }

    public record Marker(Type type, Vec3 pos, int streamId, long gameTime) {}

    public static final int LIFETIME_TICKS = 20;

    private static boolean ENABLED = false;
    private static final List<Marker> markers = new ArrayList<>();

    private EtherStreamSyncMarker() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static void setEnabled(boolean enabled) {
        ENABLED = enabled;
    }

    public static void record(Type type, Vec3 pos, int streamId) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        markers.add(new Marker(type, pos, streamId, mc.level.getGameTime()));
    }

    public static void tick() {
        if (markers.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        markers.removeIf(m -> now - m.gameTime() > LIFETIME_TICKS);
    }

    public static List<Marker> getMarkers() {
        return markers;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `idea_build_project` (filesToRebuild: `src/main/java/studio/fantasyit/ether_craft/client/debug/EtherStreamSyncMarker.java`)
Expected: BUILD SUCCESSFUL，无编译错误。

- [ ] **Step 3: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/client/debug/EtherStreamSyncMarker.java
git commit -m "feat: add EtherStreamSyncMarker static marker store"
```

---

### Task 2: 创建 `EtherStreamSyncDebugRenderer`

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/client/debug/EtherStreamSyncDebugRenderer.java`

渲染器实现原版 `SimpleDebugRenderer`。颜色与文字标签按 `Type` 区分，剩余寿命比例控制 shrink 与 alpha。

- [ ] **Step 1: 创建文件**

```java
package studio.fantasyit.ether_craft.client.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EtherStreamSyncDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final int CREATE_COLOR = ARGB.colorFromFloat(1.0F, 0.0F, 1.0F, 0.0F);
    private static final int UPDATE_COLOR = ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 0.0F);
    private static final int DELETE_COLOR = ARGB.colorFromFloat(1.0F, 1.0F, 0.0F, 0.0F);

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        if (!EtherStreamSyncMarker.isEnabled()) return;
        long now = net.minecraft.client.Minecraft.getInstance().level.getGameTime();
        for (EtherStreamSyncMarker.Marker marker : EtherStreamSyncMarker.getMarkers()) {
            int age = (int) (now - marker.gameTime());
            float progress = 1.0F - Math.max(0, age) / (float) EtherStreamSyncMarker.LIFETIME_TICKS;
            if (progress <= 0.0F) continue;
            int color = switch (marker.type()) {
                case CREATE -> CREATE_COLOR;
                case UPDATE -> UPDATE_COLOR;
                case DELETE -> DELETE_COLOR;
            };
            float alpha = progress;
            float halfSize = 0.12F * progress;
            Vec3 pos = marker.pos();
            AABB box = new AABB(pos.x - halfSize, pos.y - halfSize, pos.z - halfSize,
                    pos.x + halfSize, pos.y + halfSize, pos.z + halfSize);
            Gizmos.cuboid(box, GizmoStyle.stroke(ARGB.multiplyAlpha(color, alpha)));
            String label = switch (marker.type()) {
                case CREATE -> "C";
                case UPDATE -> "U";
                case DELETE -> "D";
            } + marker.streamId();
            Gizmos.billboardText(label, pos, TextGizmo.Style.forColorAndCentered(ARGB.multiplyAlpha(color, alpha)))
                    .setAlwaysOnTop();
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `idea_build_project` (filesToRebuild: `src/main/java/studio/fantasyit/ether_craft/client/debug/EtherStreamSyncDebugRenderer.java`)
Expected: BUILD SUCCESSFUL。若 `ARGB.multiplyAlpha(int, float)` 签名不符，检查 `net.minecraft.util.ARGB` 实际签名并调整（该 mod 已引用 `ARGB`，见 `EtherCraftRenderEvent` 相关代码，必要时用 `ARGB.color(...)` 显式算色）。

- [ ] **Step 3: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/client/debug/EtherStreamSyncDebugRenderer.java
git commit -m "feat: add ether stream sync marker debug renderer"
```

---

### Task 3: 注册调试渲染器

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/client/debug/EtherStreamSyncDebugRenderer.java`

`RegisterDebugRenderersEvent` 是 `IModBusEvent`，经 `@EventBusSubscriber(modid, Dist.CLIENT)` 自动路由到 mod 总线（与该 mod 现有 `PlatingTriggerKeyHandler` 模式一致）。

- [ ] **Step 1: 在渲染器类中添加内部订阅**

在 `EtherStreamSyncDebugRenderer` 类中添加嵌套事件订阅（沿用 mod 现有 `@EventBusSubscriber` 静态方法模式）：

```java
    @net.neoforged.bus.api.SubscribeEvent
    public static void onRegisterDebugRenderers(net.neoforged.neoforge.client.event.RegisterDebugRenderersEvent event) {
        event.register(client -> new EtherStreamSyncDebugRenderer());
    }
```

类声明上方追加 `@net.neoforged.fml.common.EventBusSubscriber(modid = studio.fantasyit.ether_craft.EtherCraft.MODID, value = Dist.CLIENT)` 注解（将 `@OnlyIn(Dist.CLIENT)` 保留在类上，二者可共存）。

完整类声明变为：

```java
@OnlyIn(Dist.CLIENT)
@net.neoforged.fml.common.EventBusSubscriber(modid = studio.fantasyit.ether_craft.EtherCraft.MODID, value = Dist.CLIENT)
public class EtherStreamSyncDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
```

- [ ] **Step 2: 编译验证**

Run: `idea_build_project` (filesToRebuild: `src/main/java/studio/fantasyit/ether_craft/client/debug/EtherStreamSyncDebugRenderer.java`)
Expected: BUILD SUCCESSFUL。

- [ ] **Step 3: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/client/debug/EtherStreamSyncDebugRenderer.java
git commit -m "feat: register ether stream sync marker debug renderer"
```

---

### Task 4: 创建按键开关处理器

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/client/key/EtherStreamSyncDebugKeyHandler.java`

`KeyMapping` 默认键为 `InputConstants.UNKNOWN`（留空），切换 `EtherStreamSyncMarker.ENABLED`。沿用 `PlatingTriggerKeyHandler` 的类别与模式。

- [ ] **Step 1: 创建文件**

```java
package studio.fantasyit.ether_craft.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.client.debug.EtherStreamSyncMarker;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class EtherStreamSyncDebugKeyHandler {
    public static final KeyMapping SYNC_MARKER = new KeyMapping(
            "key.ether_craft.sync_marker",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            PlatingTriggerKeyHandler.ETHER_CRAFT_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SYNC_MARKER);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        while (SYNC_MARKER.consumeClick()) {
            EtherStreamSyncMarker.setEnabled(!EtherStreamSyncMarker.isEnabled());
        }
    }
}
```

- [ ] **Step 2: 添加语言键**

- Modify: `src/main/resources/assets/ether_craft/lang/en_us.json` — 追加：
```json
"key.ether_craft.sync_marker": "Ether Stream Sync Markers",
"key.ether_craft.category": "Ether Craft"
```
  注：检查 `en_us.json` / `zh_cn.json` 是否已有 `key.ether_craft.category`（`PlatingTriggerKeyHandler` 用到），若已有则只追加 `sync_marker` 键。
- Modify: `src/main/resources/assets/ether_craft/lang/zh_cn.json` — 追加（若存在该文件）：
```json
"key.ether_craft.sync_marker": "以太流同步标记"
```

先读取这两个 JSON 文件确认现有结构与是否存在 category 键，再以合法 JSON 合并，避免重复键。

- [ ] **Step 3: 编译验证**

Run: `idea_build_project` (filesToRebuild: `src/main/java/studio/fantasyit/ether_craft/client/key/EtherStreamSyncDebugKeyHandler.java`)
Expected: BUILD SUCCESSFUL。

- [ ] **Step 4: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/client/key/EtherStreamSyncDebugKeyHandler.java src/main/resources/assets/ether_craft/lang/
git commit -m "feat: add ether stream sync marker toggle key"
```

---

### Task 5: 在 `ClientVESHData` 接入记录点

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/stream/client/data/ClientVESHData.java`

在五个 S2C handler 中调用 `EtherStreamSyncMarker.record(...)`。位置取自对应 `ClientStreamEntry`。所有调用都被 `record()` 内部的 `if (!ENABLED) return;` 短路，关闭时零开销。

- [ ] **Step 1: 添加 import**

在 `ClientVESHData.java` 顶部添加：

```java
import studio.fantasyit.ether_craft.client.debug.EtherStreamSyncMarker;
```

- [ ] **Step 2: `handleCreate(EtherStreamInitialCreateS2C)` 接入**

现有代码：
```java
    public void handleCreate(EtherStreamInitialCreateS2C msg) {
        if (level.get() == null) return;
        ClientVESHEntry entry = createOrGet(msg.posDir());
        if (!entry.streams.containsKey(msg.streamId())) {
            entry.addStream(msg.streamId(), msg.posDir(), msg);
        }
    }
```
改为：
```java
    public void handleCreate(EtherStreamInitialCreateS2C msg) {
        if (level.get() == null) return;
        ClientVESHEntry entry = createOrGet(msg.posDir());
        if (!entry.streams.containsKey(msg.streamId())) {
            entry.addStream(msg.streamId(), msg.posDir(), msg);
            ClientStreamEntry created = entry.streams.get(msg.streamId());
            if (created != null) {
                EtherStreamSyncMarker.record(EtherStreamSyncMarker.Type.CREATE, created.currentPos, msg.streamId());
            }
        }
    }
```

- [ ] **Step 3: `handleCreate(EtherStreamBatchCreateS2C)` 接入**

现有代码：
```java
    public void handleCreate(EtherStreamBatchCreateS2C msg) {
        if (level.get() == null) return;
        ClientVESHEntry entry = createOrGet(msg.posDir());
        for (EtherStreamBatchCreateS2C.StreamEntry se : msg.entries()) {
            if (!entry.streams.containsKey(se.streamId())) {
                entry.addStream(se.streamId(), msg.posDir(), se);
            }
        }
    }
```
改为：
```java
    public void handleCreate(EtherStreamBatchCreateS2C msg) {
        if (level.get() == null) return;
        ClientVESHEntry entry = createOrGet(msg.posDir());
        for (EtherStreamBatchCreateS2C.StreamEntry se : msg.entries()) {
            if (!entry.streams.containsKey(se.streamId())) {
                entry.addStream(se.streamId(), msg.posDir(), se);
                ClientStreamEntry created = entry.streams.get(se.streamId());
                if (created != null) {
                    EtherStreamSyncMarker.record(EtherStreamSyncMarker.Type.CREATE, created.currentPos, se.streamId());
                }
            }
        }
    }
```

- [ ] **Step 4: `handleQuickCreate` 接入**

现有代码：
```java
    public void handleQuickCreate(EtherStreamQuickCreateS2C msg) {
        if (level.get() == null) return;
        ClientVESHEntry entry = entries.get(msg.posDir());
        if (entry == null || !entry.hasLast()) return;
        IEtherStreamEntryLike quickEntry = entry.getFromLastAndUpdate();
        entry.addStream(quickEntry.streamId(), msg.posDir(), quickEntry);
    }
```
改为：
```java
    public void handleQuickCreate(EtherStreamQuickCreateS2C msg) {
        if (level.get() == null) return;
        ClientVESHEntry entry = entries.get(msg.posDir());
        if (entry == null || !entry.hasLast()) return;
        IEtherStreamEntryLike quickEntry = entry.getFromLastAndUpdate();
        entry.addStream(quickEntry.streamId(), msg.posDir(), quickEntry);
        ClientStreamEntry created = entry.streams.get(quickEntry.streamId());
        if (created != null) {
            EtherStreamSyncMarker.record(EtherStreamSyncMarker.Type.CREATE, created.currentPos, quickEntry.streamId());
        }
    }
```

- [ ] **Step 5: `handleUpdate` 接入**

现有代码：
```java
    public void handleUpdate(EtherStreamUpdateS2C msg) {
        if (level.get() == null) return;
        ClientVESHEntry entry = createOrGet(msg.posDir());
        for (EtherStreamUpdateS2C.StreamEntry se : msg.entries()) {
            ClientStreamEntry current = entry.streams.get(se.streamId());
            if (current == null || current.isDying || current.removed) continue;
            current.updateFromServer(se.ether(), se.consumerState());
            current.updateDynamic();
        }
    }
```
改为：
```java
    public void handleUpdate(EtherStreamUpdateS2C msg) {
        if (level.get() == null) return;
        ClientVESHEntry entry = createOrGet(msg.posDir());
        for (EtherStreamUpdateS2C.StreamEntry se : msg.entries()) {
            ClientStreamEntry current = entry.streams.get(se.streamId());
            if (current == null || current.isDying || current.removed) continue;
            current.updateFromServer(se.ether(), se.consumerState());
            current.updateDynamic();
            EtherStreamSyncMarker.record(EtherStreamSyncMarker.Type.UPDATE, current.currentPos, se.streamId());
        }
    }
```

- [ ] **Step 6: `handleDying` 接入**

现有代码：
```java
    public void handleDying(EtherStreamSetDyingS2C msg) {
        Level lv = level.get();
        if (lv == null) return;
        ClientVESHEntry entry = createOrGet(msg.posDir());
        long levelTime = lv.getGameTime();
        for (int sid : msg.entries()) {
            ClientStreamEntry current = entry.streams.get(sid);
            if (current == null) continue;

            if (current.attachedLogic.stream().anyMatch(t -> t.shouldDelayDeath(current))) {
                current.setDying();
                current.deathAtTick = levelTime;
                current.updateDynamic();
            } else {
                current.setRemoved();
            }
        }
    }
```
改为（在记录前先读取位置，因为 `setRemoved` 后位置仍可用，`currentPos` 不会被清空）：
```java
    public void handleDying(EtherStreamSetDyingS2C msg) {
        Level lv = level.get();
        if (lv == null) return;
        ClientVESHEntry entry = createOrGet(msg.posDir());
        long levelTime = lv.getGameTime();
        for (int sid : msg.entries()) {
            ClientStreamEntry current = entry.streams.get(sid);
            if (current == null) continue;

            EtherStreamSyncMarker.record(EtherStreamSyncMarker.Type.DELETE, current.currentPos, sid);

            if (current.attachedLogic.stream().anyMatch(t -> t.shouldDelayDeath(current))) {
                current.setDying();
                current.deathAtTick = levelTime;
                current.updateDynamic();
            } else {
                current.setRemoved();
            }
        }
    }
```

- [ ] **Step 7: `handleSync` 接入**

现有代码：
```java
    public void handleSync(EtherStreamSyncDataS2C etherStreamSyncDataS2C) {
        if (level.get() == null) return;
        ClientVESHEntry ent = createOrGet(etherStreamSyncDataS2C.posDir());
        if (ent == null) return;
        if (ent.streams.containsKey(etherStreamSyncDataS2C.streamId())) {
            ClientStreamEntry entry = ent.streams.get(etherStreamSyncDataS2C.streamId());
            entry.syncedData.clear();
            for (IEtherStreamSyncedData data : etherStreamSyncDataS2C.data())
                entry.syncedData.put(data.getId(), data);
            entry.updateDynamic();
        }
    }
```
改为：
```java
    public void handleSync(EtherStreamSyncDataS2C etherStreamSyncDataS2C) {
        if (level.get() == null) return;
        ClientVESHEntry ent = createOrGet(etherStreamSyncDataS2C.posDir());
        if (ent == null) return;
        if (ent.streams.containsKey(etherStreamSyncDataS2C.streamId())) {
            ClientStreamEntry entry = ent.streams.get(etherStreamSyncDataS2C.streamId());
            entry.syncedData.clear();
            for (IEtherStreamSyncedData data : etherStreamSyncDataS2C.data())
                entry.syncedData.put(data.getId(), data);
            entry.updateDynamic();
            EtherStreamSyncMarker.record(EtherStreamSyncMarker.Type.UPDATE, entry.currentPos, etherStreamSyncDataS2C.streamId());
        }
    }
```

- [ ] **Step 8: 编译验证**

Run: `idea_build_project` (filesToRebuild: `src/main/java/studio/fantasyit/ether_craft/stream/client/data/ClientVESHData.java`)
Expected: BUILD SUCCESSFUL。

- [ ] **Step 9: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/stream/client/data/ClientVESHData.java
git commit -m "feat: record sync markers on ether stream S2C handlers"
```

---

### Task 6: 在 `event/ClientTickEvent` 驱动 `tick()` 清理

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/event/ClientTickEvent.java`

在现有客户端 level tick 末尾追加 `EtherStreamSyncMarker.tick()`。

- [ ] **Step 1: 添加 import 并接入**

添加 import：
```java
import studio.fantasyit.ether_craft.client.debug.EtherStreamSyncMarker;
```

现有代码：
```java
        if (mc.level == null) return;
        ClientVESHData.getWithCurrentLevel(mc.level).tick();
        EntityStreamClientManager.tick();
```
改为：
```java
        if (mc.level == null) return;
        ClientVESHData.getWithCurrentLevel(mc.level).tick();
        EntityStreamClientManager.tick();
        EtherStreamSyncMarker.tick();
```

- [ ] **Step 2: 编译验证**

Run: `idea_build_project` (filesToRebuild: `src/main/java/studio/fantasyit/ether_craft/event/ClientTickEvent.java`)
Expected: BUILD SUCCESSFUL。

- [ ] **Step 3: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/event/ClientTickEvent.java
git commit -m "feat: tick expire sync markers in client tick"
```

---

### Task 7: 全量构建 + 运行验证

**Files:** 无（验证任务）

- [ ] **Step 1: 全量构建**

Run: `idea_build_project` (rebuild: true)
Expected: BUILD SUCCESSFUL，无错误。

- [ ] **Step 2: 运行客户端冒烟验证**

Run: `idea_execute_run_configuration`（选择 runClient，或运行配置列表中的 Client 配置）
Expected: 客户端正常启动。

手动验证步骤（需要人工，记录到 README 或提交说明即可）：
1. 打开控制设置，为 "Ether Stream Sync Markers" 绑定一个按键（如 `Y`）。
2. 在创造模式放置 `Ether Stream Emitter` 并注入以太，发射流。
3. 按绑定键开启调试层：流被创建时在流位置出现绿色 `C<id>` 方框，ether 更新时出现黄色 `U<id>`，流消亡时出现红色 `D<id>`。
4. 每个标记 1 秒内 shrink 淡出消失。
5. 再次按键关闭：不再出现任何标记，游戏表现与关闭前一致（零开销）。

- [ ] **Step 3: 最终提交（如有残留改动）**

```bash
git status
# 如有未提交改动，git add 并 commit
```

---

## Self-Review 记录

**1. Spec coverage:**
- `EtherStreamSyncMarker`（静态存储 / ENABLED / record / tick / getMarkers）→ Task 1 ✓
- `EtherStreamSyncDebugRenderer`（SimpleDebugRenderer，三色 cuboid + 文字标签，shrink/淡出）→ Task 2 ✓
- 渲染器注册（RegisterDebugRenderersEvent）→ Task 3 ✓
- `EtherStreamSyncDebugKeyHandler`（KeyMapping 默认留空，切换 ENABLED）→ Task 4 ✓
- `ClientVESHData` 五个 handler 接入 → Task 5 ✓
- `tick()` 驱动（event/ClientTickEvent）→ Task 6 ✓
- 零开销保证（record/emitGizmos 首行短路）→ Task 1 `record`、Task 2 `emitGizmos`、Task 5 说明 ✓
- 无上限、20 tick 过期 → Task 1 ✓

**2. Placeholder scan:** 无 TBD/TODO；所有步骤含完整代码与命令。Task 4 语言键部分依赖现有 JSON 内容，已给出明确"先读取再合并"指令，非占位符。

**3. Type consistency:** `EtherStreamSyncMarker.Type` 三个枚举值 `CREATE/UPDATE/DELETE` 在 Task 1 定义，Task 2/5 使用一致；`record(Type, Vec3, int)` 签名一致；`Marker` record 字段 `type/pos/streamId/gameTime` 使用一致；`LIFETIME_TICKS = 20` 全程一致。`ClientStreamEntry.currentPos` 在 Task 5 全程使用，字段存在（`ClientStreamEntry.java:26`）。
