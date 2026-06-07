# Plating Effects Expansion — Design Spec

**Date**: 2026-06-08
**Status**: Approved

## Overview

Add 5 new plating effects to the existing plating system, refactor the `IPlatingEffect` interface
into granular trigger interfaces, and consolidate all plating-related code into the `plating/` package.

### New Effects

| Effect ID | Name | Description |
|-----------|------|-------------|
| `ether_craft:dash` | Dash | Right-click to burst forward in view direction. CD 20t, consumes ether. |
| `ether_craft:high_jump` | High Jump | Right-click to launch upward. Fall damage reduction like Jump Boost. CD 20t, consumes ether. |
| `ether_craft:soul_projection` | Soul Projection | Right-click toggle — camera detaches, free-flight through blocks (WASD), 64-block range limit. Body stays visible and vulnerable. Per-tick ether drain. |
| `ether_craft:no_gravity` | No Gravity | Bow/crossbow arrows have no gravity. Consumes ether per arrow. |
| `ether_craft:coyote_time` | Coyote Time | Can jump up to 2 seconds after walking off a block edge. Passive while equipped. Consumes ether per jump. |
| `ether_craft:damage` (existing) | Damage | Bonus damage on attack. Unchanged logic. |

---

## Architecture

### 1. Trigger Interface Refactoring

The monolithic `IPlatingEffect` is split into granular trigger interfaces under `plating/trigger/`.
Each trigger interface extends `IPlatingEffect`.

#### `IPlatingEffect` (base — `plating/effects/IPlatingEffect.java`)

```java
public interface IPlatingEffect {
    double getEffectByEther(long ether);
    default long getCdTicks() { return 0; }
}
```

#### Trigger Interfaces (`plating/trigger/`)

| Interface | Method | Source | New? |
|-----------|--------|--------|------|
| `IPlatingAttackTrigger` | `onAttack(data, stack, player, target) → boolean` | existing | |
| `IPlatingBreakBlockTrigger` | `onBreakBlock(data, stack, player, pos, state) → boolean` | existing | |
| `IPlatingUseTrigger` | `onUse(data, stack, player)` | existing | |
| `IPlatingUseOnBlockTrigger` | `onUseOnBlock(data, stack, player, pos, state)` | existing | |
| `IPlatingUseOnEntityTrigger` | `onUseOnEntity(data, stack, player, target)` | existing | |
| `IPlatingHoldTickTrigger` | `onHoldTick(data, stack, player)` | existing | |
| `IPlatingRightClickTrigger` | `onRightClick(data, stack, player) → boolean` | — | **new** |
| `IPlatingArrowShotTrigger` | `onArrowShot(data, stack, player, arrow)` | — | **new** |
| `IPlatingJumpTrigger` | `canJump(data, stack, player) → boolean` | — | **new** |

`PlatingEventHandler` is rewritten to dispatch via `instanceof` checks against trigger interfaces.

### 2. `PlatingData` Changes

Add `coolDownUntil` field for per-effect cooldown tracking directly on the item stack.

```java
public record PlatingData(Identifier id, double effect, @Nullable Long coolDownUntil) {
    public PlatingData(Identifier id, double effect) {
        this(id, effect, null);
    }

    public PlatingData copyWithCoolDown(ServerLevel level, long cdTicks) {
        return new PlatingData(id, effect, level.getGameTime() + cdTicks);
    }

    public boolean isCd(ServerLevel level) {
        return coolDownUntil != null && level.getGameTime() < coolDownUntil;
    }
}
```

`PlatingUtil` gains `updatePlatingData(ItemStack, PlatingData)` to replace one entry in the immutable list.

### 3. Effect Implementations (`plating/effects/`)

#### DashPlatingEffect
- Implements: `IPlatingRightClickTrigger`
- `onRightClick`: check CD + extract ether → `player.setDeltaMovement(look * effect * 0.5, 0.1, look * effect * 0.5)` → `copyWithCoolDown` → `updatePlatingData`
- `getCdTicks()`: returns `Config.platingDashCdTicks`
- Ether cost: `Config.platingDashEtherCost`

#### HighJumpPlatingEffect
- Implements: `IPlatingRightClickTrigger`
- `onRightClick`: check CD + extract ether → `player.setDeltaMovement(0, effect * 1.0, 0)` → apply JumpBoost-style fall damage reduction → `copyWithCoolDown`
- Fall damage reduction: give `JumpEffect` at amplifier 0 for 3 seconds (or configurable)
- `getCdTicks()`: returns `Config.platingHighJumpCdTicks`
- Ether cost: `Config.platingHighJumpEtherCost`

#### SoulProjectionPlatingEffect
- Implements: `IPlatingRightClickTrigger`, `IPlatingHoldTickTrigger`
- `onRightClick`: toggle activation state in `PlatingPlayerAttachment` → send `PlatingSoulStateS2C` to client
- `onHoldTick`: if active, extract `Config.platingSoulEtherPerTick` ether per tick → check range limit (`Config.platingSoulMaxRange`) → if exceeded, auto-deactivate
- Client: `PlatingClientEventHandler` handles camera detachment via `CameraSetup`, WASD input for free-flight within range
- No cooldown on the PlatingData stack (state is in attachment)

#### NoGravityPlatingEffect
- Implements: `IPlatingArrowShotTrigger`
- `onArrowShot`: extract `Config.platingNoGravityEtherPerArrow` ether → `arrow.setNoGravity(true)`
- Triggered in `PlatingEventHandler` via `ArrowLooseEvent` — check player's held bow/crossbow for plating
- Arrow only (not trident/snowball)

#### CoyoteTimePlatingEffect
- Implements: `IPlatingJumpTrigger`, `IPlatingHoldTickTrigger`
- `onHoldTick`: if player is on ground, record `lastOnGroundTime` in `PlatingPlayerAttachment`
- `canJump`: if `now - lastOnGroundTime <= 40` ticks, extract `Config.platingCoyoteTimeEtherPerJump` ether → return true
- Mixin `LivingEntity.jumpFromGround()` calls `canJump` check via static helper

### 4. DamagePlatingEffect Update
- Changed from `implements IPlatingEffect` to `implements IPlatingAttackTrigger`
- Removed `@Override` annotation from interface method — becomes the trigger method

---

## Package Layout

### `plating/` (final)

```
plating/
├── PlatingData.java                     (MODIFIED: +coolDownUntil)
├── PlatingUtil.java                     (MODIFIED: +updatePlatingData)
├── PlatingManager.java                  (MODIFIED: init() registers 5 new effects)
│
├── effects/
│   ├── IPlatingEffect.java              (MODIFIED: only getEffectByEther + getCdTicks)
│   ├── DamagePlatingEffect.java         (MODIFIED: implements IPlatingAttackTrigger)
│   ├── DashPlatingEffect.java           [NEW]
│   ├── HighJumpPlatingEffect.java       [NEW]
│   ├── SoulProjectionPlatingEffect.java [NEW]
│   ├── NoGravityPlatingEffect.java      [NEW]
│   └── CoyoteTimePlatingEffect.java     [NEW]
│
├── trigger/                             [NEW]
│   ├── IPlatingAttackTrigger.java
│   ├── IPlatingBreakBlockTrigger.java
│   ├── IPlatingUseTrigger.java
│   ├── IPlatingUseOnBlockTrigger.java
│   ├── IPlatingUseOnEntityTrigger.java
│   ├── IPlatingHoldTickTrigger.java
│   ├── IPlatingRightClickTrigger.java
│   ├── IPlatingArrowShotTrigger.java
│   └── IPlatingJumpTrigger.java
│
├── event/                               [MOVED IN + NEW]
│   ├── PlatingEventHandler.java         (from event/; rewritten with instanceof dispatch)
│   └── PlatingItemEntityTicker.java     (extracted from ItemEntityTickEvent)
│
├── client/                              [MOVED IN + NEW]
│   ├── PlatingTooltipHandler.java       (from event/)
│   └── PlatingClientEventHandler.java   [NEW]
│
└── attachment/                          [NEW]
    └── PlatingPlayerAttachment.java
```

### External Files

```
mixin/
└── plating/
    └── LivingEntityJumpMixin.java       [NEW]

network/
├── c2s/
│   └── PlatingTriggerC2S.java           [NEW]
├── s2c/
│   └── PlatingSoulStateS2C.java         [NEW]
└── Network.java                         (MODIFIED: register 2 new packets)

register/
└── AttachmentRegistry.java              [NEW] (if not already present)

Config.java                              (MODIFIED: +8 config entries)

event/
└── ItemEntityTickEvent.java             (MODIFIED: delegate tickPlating to PlatingItemEntityTicker)
```

---

## Config Entries

```java
// === plating.dash ===
plating.dash.cd_ticks           = 20
plating.dash.ether_cost          = 5

// === plating.high_jump ===
plating.high_jump.cd_ticks       = 20
plating.high_jump.ether_cost     = 5

// === plating.soul ===
plating.soul.ether_per_tick      = 1
plating.soul.max_range           = 64

// === plating.no_gravity ===
plating.no_gravity.ether_per_arrow = 1

// === plating.coyote_time ===
plating.coyote_time.ether_per_jump = 1
```

All existing config entries remain unchanged.

---

## Network Packets

### `PlatingTriggerC2S` (c2s)
- Fields: `Identifier effectId`
- Server handler: iterate equipped items, find plating matching effectId, dispatch to `IPlatingRightClickTrigger.onRightClick()`
- Used by: Dash, HighJump, SoulProjection activation/deactivation

### `PlatingSoulStateS2C` (s2c)
- Fields: `boolean active`
- Client handler: `PlatingClientEventHandler` toggles camera detachment + input redirection
- Sent when server activates or deactivates soul projection

---

## Attachment

### `PlatingPlayerAttachment`
- `soulActive` (boolean) — whether soul projection is currently active
- `soulCameraPos` (Vec3) — current free-fly camera position (server-side for range check)
- `lastOnGroundTick` (long) — gameTime when player was last on ground (for coyote time)
- Registered as a player data attachment via NeoForge's `AttachmentType`

---

## Event Flow

### Dash / HighJump (right-click trigger)
1. Client: `PlayerInteractEvent.RightClickItem` → check player's held item for plating → if `IPlatingRightClickTrigger` → cancel event → send `PlatingTriggerC2S`
2. Server: receive C2S → find plating data on equipped items → `instanceof IPlatingRightClickTrigger trigger → trigger.onRightClick(data, stack, player)`
3. Effect: check CD, extract ether, apply movement, update `coolDownUntil`

### Soul Projection
1. Client: right-click (same as above) → `PlatingTriggerC2S`
2. Server: `SoulProjectionPlatingEffect.onRightClick()` → toggle `PlatingPlayerAttachment.soulActive` → send `PlatingSoulStateS2C` → if active start per-tick drain in `onHoldTick`
3. Client receives S2C:
   - Active: `Minecraft.setCameraEntity(null)` → use `CameraSetup` event to set third-person detached position → intercept input for WASD free-flight
   - Inactive: `Minecraft.setCameraEntity(player)` → restore normal view
4. Per-tick (server): `onHoldTick` drains ether, checks range (`soulCameraPos.distanceTo(player.pos) > maxRange`)
5. Right-click while active: deactivates

### No Gravity
1. Server: `ArrowLooseEvent` → check player's held bow/crossbow for plating → `instanceof IPlatingArrowShotTrigger trigger → trigger.onArrowShot(data, stack, player, arrow)`
2. Effect: extract ether, set arrow no-gravity

### Coyote Time
1. Server tick: `onHoldTick` → if player.isOnGround(), set `PlatingPlayerAttachment.lastOnGroundTick`
2. Mixin `LivingEntity.jumpFromGround()` → hook calls `PlatingEventHandler.tryJump(player)` → iterate equipped items → `instanceof IPlatingJumpTrigger trigger → trigger.canJump(data, stack, player)`
3. Effect: check `now - lastOnGroundTick <= 40`, extract ether, return true

---

## Files to Create (17 new)

| File | Package |
|------|---------|
| `plating/trigger/IPlatingAttackTrigger.java` | `...plating.trigger` |
| `plating/trigger/IPlatingBreakBlockTrigger.java` | `...plating.trigger` |
| `plating/trigger/IPlatingUseTrigger.java` | `...plating.trigger` |
| `plating/trigger/IPlatingUseOnBlockTrigger.java` | `...plating.trigger` |
| `plating/trigger/IPlatingUseOnEntityTrigger.java` | `...plating.trigger` |
| `plating/trigger/IPlatingHoldTickTrigger.java` | `...plating.trigger` |
| `plating/trigger/IPlatingRightClickTrigger.java` | `...plating.trigger` |
| `plating/trigger/IPlatingArrowShotTrigger.java` | `...plating.trigger` |
| `plating/trigger/IPlatingJumpTrigger.java` | `...plating.trigger` |
| `plating/effects/DashPlatingEffect.java` | `...plating.effects` |
| `plating/effects/HighJumpPlatingEffect.java` | `...plating.effects` |
| `plating/effects/SoulProjectionPlatingEffect.java` | `...plating.effects` |
| `plating/effects/NoGravityPlatingEffect.java` | `...plating.effects` |
| `plating/effects/CoyoteTimePlatingEffect.java` | `...plating.effects` |
| `plating/event/PlatingItemEntityTicker.java` | `...plating.event` |
| `plating/client/PlatingClientEventHandler.java` | `...plating.client` |
| `plating/attachment/PlatingPlayerAttachment.java` | `...plating.attachment` |
| `mixin/plating/LivingEntityJumpMixin.java` | `...mixin.plating` |
| `network/c2s/PlatingTriggerC2S.java` | `...network.c2s` |
| `network/s2c/PlatingSoulStateS2C.java` | `...network.s2c` |

## Files to Modify (9 existing)

| File | Change |
|------|--------|
| `plating/PlatingData.java` | Add `coolDownUntil`, `copyWithCoolDown()`, `isCd()` |
| `plating/PlatingUtil.java` | Add `updatePlatingData()` |
| `plating/PlatingManager.java` | Register 5 new effects in `init()` |
| `plating/effects/IPlatingEffect.java` | Strip to `getEffectByEther` + `getCdTicks` |
| `plating/effects/DamagePlatingEffect.java` | Change to `implements IPlatingAttackTrigger` |
| `event/PlatingEventHandler.java` → `plating/event/PlatingEventHandler.java` | Move + rewrite dispatch logic; add new event listeners |
| `event/PlatingTooltipHandler.java` → `plating/client/PlatingTooltipHandler.java` | Move file |
| `event/ItemEntityTickEvent.java` | Extract `tickPlating` → delegate to `PlatingItemEntityTicker` |
| `network/Network.java` | Register 2 new packets |
| `Config.java` | Add 8 new config entries |
| `ether_craft.mixins.json` | Register `LivingEntityJumpMixin` |
