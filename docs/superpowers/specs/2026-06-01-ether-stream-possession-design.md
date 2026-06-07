# 以太流附身 — 设计规格

> 日期: 2026-06-01
> 状态: 设计完成，待实现

## 概述

玩家走进以太流范围时自动"附身"到流上。服务端玩家无碰撞并随流移动，客户端不渲染该玩家模型，第一人称视角跟随流移动。

## 行为约定

| 项目 | 约定 |
|------|------|
| 触发方式 | 玩家走进以太流 AABB 范围 → 自动附身 |
| 绑定关系 | 一对一（一支流 ← 一个玩家） |
| 视角 | 第一人称，相机随流移动 |
| 操控 | 不可移动、不可交互 |
| 伤害 | 免疫一切伤害 |
| 物品栏/状态 | 不受影响（保留但不可操作） |
| 退出方式 | 流死亡（到死亡位置退出）/ 手动按潜行键退出 |
| 流死亡位置 | 直接出现在流消失的位置 |

## 核心架构

**双端独立位置计算**：服务端和客户端各自根据已有的流数据独立计算玩家位置，无需自定义位置同步网络包。

```
每 Tick:

  Server:
    VES.tick() → pos += motion          [现有逻辑]
    PossessionManager.tick()
      → player.noPhysics = true
      → player.setPos(ves.pos)
      → ServerEntity 自动推位置给所有追踪客户端 [vanilla]

  Client (附身者):
    ClientVESHData.tick() → calc pos    [现有逻辑]
    PossessionClientHandler.tick()
      → localPlayer.setPos(streamPos)   // 相机跟随
      → localPlayer.input = new FrozenClientInput() // 抑制移动

  Client (所有人):
    RenderPlayerEvent.Pre → cancel (若玩家正在附身)
    RenderNameTagEvent.DoRender → cancel
```

## 模块清单

### 1. 状态存储

**文件**: `attachment/PossessionData.java`（新建）

```java
// DataAttachment<PosDir> 附着在玩家身上
// PosDir = 被附身的流的源方块位置 + 方向，用于定位流
public static final DataAttachment<PosDir> POSSESSION = ...;
```

**文件**: `stream/vholder/VirtualEtherStream.java`（修改）

- 新增字段 `@Nullable UUID possessingPlayerUUID`
- 新增方法 `isPossessed()` / `setPossessingPlayer(UUID)` / `clearPossessingPlayer()`

### 2. 流碰撞拦截（附身触发）

**文件**: `stream/vholder/VirtualEtherStreamHolder.java`（修改）

- 修改 `tick()` 中实体碰撞处理段（约第 193-198 行）
- 检测到碰撞实体为 `ServerPlayer` 时：
  - 检查该玩家是否已有附身（`PossessionData`）
  - 若没有：设置附身绑定，跳过 `markDead()`
  - 若已附身其他流：忽略（或替换，取决于设计决策）

### 3. 服务端位置跟随

**文件**: `event/ServerTickHandler.java`（修改）或新建 `event/PlayerPossessionTickHandler.java`

- `ServerTickEvent.Post` 中，`VirtualEtherStreamHolderManager` tick 之后
- 遍历所有附身玩家 → `player.setPos(ves.pos)`
- 设置 `player.noPhysics = true`
- 可选：设置 `player.setDeltaMovement(ves.motion)` 以保持运动状态

### 4. 客户端位置跟随 + 输入抑制

**文件**: `client/FrozenClientInput.java`（新建）

```java
public class FrozenClientInput extends ClientInput {
    public FrozenClientInput() {
        this.keyPresses = Input.EMPTY;   // public
        this.moveVector = Vec2.ZERO;     // protected, subclass accessible
    }
}
```

**文件**: `event/ClientTickEvent.java`（修改）或新建事件类

- `ClientTickEvent.Post` 中，检查本地玩家是否附身
- `localPlayer.setPos(clientStreamEntry.position())`
- `localPlayer.input = new FrozenClientInput()`
- 监听 `MovementInputUpdateEvent` → 替换 `player.input` 为 `FrozenClientInput`

### 5. 渲染隐藏

**文件**: `event/ClientRenderHandler.java`（新建）

```java
@EventBusSubscriber(modid = "ether_craft", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientRenderHandler {

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (isPossessing(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent.DoRender event) {
        if (isPossessing(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
```

- `isPossessing()` 检查本地维护的 `Set<UUID> possessingPlayers`

### 6. 伤害/交互豁免

**文件**: `event/ServerTickHandler.java` 或新建事件类

```java
@SubscribeEvent
public static void onEntityDamage(LivingIncomingDamageEvent event) {
    if (event.getEntity() instanceof Player player && isPossessing(player)) {
        event.setCanceled(true);
    }
}
```

- 或使用 `EntityInvulnerabilityCheckEvent` 替代

### 7. 网络包

**文件**: `network/s2c/PossessStartS2C.java`（新建）

```
PossessStartS2C(playerUUID: UUID, streamPosDir: PosDir)
→ 维度广播：告知客户端某玩家开始附身，加入 possessingPlayers 集合
```

**文件**: `network/s2c/PossessStopS2C.java`（新建）

```
PossessStopS2C(playerUUID: UUID)
→ 维度广播：告知客户端某玩家退出附身，从 possessingPlayers 集合移除
// 不需要携带位置信息——退出时服务端的 teleportTo() 已通过 vanilla 同步
```

**文件**: `network/s2c/PossessSyncS2C.java`（新建）

```
PossessSyncS2C(List<UUID, PosDir>)
→ 发给新加入维度/进服的玩家，告知当前所有附身状态
```

**文件**: `network/c2s/UnpossessC2S.java`（新建）

```
UnpossessC2S()
→ 客户端按潜行键时发送，请求手动退出附身
```

**网络注册**: `network/Network.java`（修改）

- 注册上述 3 个 S2C + 1 个 C2S
- 广播方式：`PacketDistributor.sendToPlayersInDimension(serverLevel, ...)`
- 注意：维度级广播而非追踪实体范围，确保所有客户端都知道附身状态

### 8. 流死亡通知 / 玩家下线处理

**文件**: `stream/vholder/VirtualEtherStream.java`（修改）

- `markDead()` 中检查 `possessingPlayerUUID`
- 若有：获取玩家 → 清理附身状态 → `player.teleportTo(ves.pos)` → 发 `PossessStopS2C`

**文件**: 新建或修改事件类

- 监听 `PlayerEvent.PlayerLoggedOutEvent` → 如果该玩家正在附身，清理绑定并通知

## 关键 API 速查

| 用途 | API |
|------|-----|
| 消除碰撞 | `player.noPhysics = true`（`Entity` public 字段） |
| 服务端设位置 | `player.setPos(x, y, z)` 或 `player.teleportTo(x, y, z)`（退出时） |
| 客户端设位置 | `localPlayer.setPos(x, y, z)` |
| 抑制移动输入 | 替换 `localPlayer.input` 为 `FrozenClientInput` 实例 |
| 隐藏渲染 | `RenderPlayerEvent.Pre.setCanceled(true)` |
| 隐藏名牌 | `RenderNameTagEvent.DoRender.setCanceled(true)` |
| 拦截伤害 | `LivingIncomingDamageEvent.setCanceled(true)` |
| 状态存储 | `DataAttachment<PosDir>` 附着于 Player |
| 维度广播 | `PacketDistributor.sendToPlayersInDimension(serverLevel, payload)` |
| 新玩家同步 | 监听 `PlayerEvent.PlayerLoggedInEvent` → 发 `PossessSyncS2C` |

## 涉及的文件

### 新建

| 文件 | 说明 |
|------|------|
| `attachment/PossessionData.java` | 附身状态 DataAttachment |
| `client/FrozenClientInput.java` | 冻结输入的 ClientInput 子类 |
| `event/ClientRenderHandler.java` | 客户端渲染隐藏事件处理 |
| `network/s2c/PossessStartS2C.java` | 附身开始通知 |
| `network/s2c/PossessStopS2C.java` | 附身结束通知 |
| `network/s2c/PossessSyncS2C.java` | 新玩家同步 |
| `network/c2s/UnpossessC2S.java` | 手动退出请求 |

### 修改

| 文件 | 修改内容 |
|------|---------|
| `stream/vholder/VirtualEtherStream.java` | 新增 `possessingPlayerUUID` 字段 + markDead 通知 |
| `stream/vholder/VirtualEtherStreamHolder.java` | 实体碰撞段 → 附身而非 markDead |
| `event/ServerTickHandler.java` | 附身玩家位置跟随 tick |
| `event/ClientTickEvent.java` | 附身玩家客户端位置跟随 + 输入冻结 |
| `network/Network.java` | 注册 4 个新网络包 |

## 不需要的做法

- **不需要 Mixin**：所有行为通过 NeoForge 事件和公共 API 实现
- **不需要自定义位置同步包**：位置同步走 vanilla `ServerEntity` 追踪 + 双端独立计算
- **不需要 `teleportTo()` 每 tick**：运行时用 `setPos()`，退出时用 `teleportTo()`
