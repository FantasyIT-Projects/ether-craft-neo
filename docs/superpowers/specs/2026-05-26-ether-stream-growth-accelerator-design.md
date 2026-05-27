# Ether Stream Growth Accelerator — Design Spec

**Date:** 2026-05-26  
**Status:** Approved

## Goal

Add a stream-level growth acceleration capability (催生) that bonemeals eligible crop blocks as the ether stream passes through them. Modelled after the existing EAN `FunctionGrowthAccelerator`, but applied to the stream entity rather than the node block.

## Requirements

- Range fixed at 0: only affects the block the stream is currently at (no area scanning)
- Works identically to `FunctionGrowthAccelerator`: checks `CROP_ACCELERATABLE` tag, calls `state.randomTick()`
- Ether consumption via config value (`ether_stream.growth_accelerator.ether_cost`, default 100)
- Stream passes through crop blocks (treats them as pass-through for collision)
- Installed via node upgrade slot using Bone Meal item
- No stacking: multiple copies of the upgrade on the same node are a no-op

## Architecture

### Files Changed (2)

| File | Change |
|------|--------|
| `stream/IStreamCapability.java` | Add `default boolean shouldPassThrough(BlockState)` |
| `EtherStreamEntity.java` | In `onHit()`, iterate capabilities for dynamic pass-through after tag check |
| `Config.java` | Add `etherStreamGrowthAcceleratorEtherCost` config field |
| `node/NodePluginManager.java` | Register new `PluginInfo` in static initializer |

### Files Created (2)

| File | Role |
|------|------|
| `stream/EtherStreamGrowthAcceleratorCapability.java` | `IStreamCapability` impl — bonemeals blocks at stream position each tick, declares crops as pass-through |
| `node/plugins/upgrade/EtherStreamGrowthAcceleratorUpgrade.java` | Node upgrade plugin — extends `AbstractNodePlugin`, implements `IEtherStreamCapabilityProviderPlugin` |

## Component Details

### IStreamCapability — new method

```java
default boolean shouldPassThrough(BlockState blockState) {
    return false;
}
```

Backward-compatible default. Only `EtherStreamGrowthAcceleratorCapability` overrides it.

### EtherStreamEntity — modified onHit

After the existing tag check (`Tags.ETHER_STREAM_PASS_THROUGH`), add a loop:

```java
for (IStreamCapability cap : capabilities) {
    if (cap.shouldPassThrough(blockState)) {
        return; // treat as pass-through, no collision
    }
}
```

This runs on both client and server sides (the existing tag check is also dual-sided).

### EtherStreamGrowthAcceleratorCapability

- `getId()` → `EtherCraft.id("growth_accelerator_stream")`
- `getConsumption()` → `0`
- `shouldPassThrough(state)` → `state.is(Tags.CROP_ACCELERATABLE)`
- `tick(streamEntity)`:
  1. Guard: server-side only (`ServerLevel`)
  2. Get block at stream's `BlockPos` (current position)
  3. If `CROP_ACCELERATABLE` → read `Config.etherStreamGrowthAcceleratorEtherCost`
  4. If `streamEntity.getEther() < cost` → return (skip, try next tick)
  5. `streamEntity.consumeEther(cost)`, `state.randomTick(level, pos, random)`
- `hitBlock()` → `return false`
- `hitEntity()` → `return false`
- `onDestroy()` → no-op
- `ValueIOSerializable` — ether cost is config-based, nothing to serialize

### EtherStreamGrowthAcceleratorUpgrade

```java
public class EtherStreamGrowthAcceleratorUpgrade extends AbstractNodePlugin
        implements IEtherStreamCapabilityProviderPlugin {
    public static final Identifier ID = EtherCraft.id("growth_accelerator_upgrade");

    // Constructor: super(nodeEntity, installedId)

    @Override
    public void provideCapabilities(EtherStreamEntity entity) {
        Optional<IStreamCapability> existing = entity.getCapability(
            EtherStreamGrowthAcceleratorCapability.ID);
        if (existing.isEmpty()) {
            entity.addCapability(new EtherStreamGrowthAcceleratorCapability());
        }
    }
}
```

### Config

```java
// Private spec:
private static final ModConfigSpec.IntValue ETHER_STREAM_GROWTH_ACCELERATOR_ETHER_COST = BUILDER
    .comment("Ether consumed per crop block accelerated by Ether Stream Growth Accelerator capability")
    .defineInRange("ether_stream.growth_accelerator.ether_cost", 100, 1, Integer.MAX_VALUE);

// Public static field:
public static int etherStreamGrowthAcceleratorEtherCost;

// In onLoad():
etherStreamGrowthAcceleratorEtherCost = ETHER_STREAM_GROWTH_ACCELERATOR_ETHER_COST.get();
```

Config key: `ether_stream.growth_accelerator.ether_cost`

### NodePluginManager registration

```java
ALL_PLUGINS.add(new PluginInfo(
    PluginType.UPGRADE,
    EtherStreamGrowthAcceleratorUpgrade.ID,
    EtherStreamGrowthAcceleratorUpgrade::new,
    t -> t.is(Items.BONE_MEAL),
    Items.BONE_MEAL
));
```

Note: Bone Meal already matches `FunctionGrowthAccelerator` (FUNCTION type). The type filter (`FEATURE_UPGRADE_TYPE`) in the upgrade container accepts both `FEATURE` and `UPGRADE` types, so placing Bone Meal in an upgrade slot will match the new UPGRADE entry. Placing Bone Meal in the function slot will continue to match the FUNCTION entry. Both can coexist on the same node.

## Data Flow

```
Player places Bone Meal in upgrade slot
  → EtherPluginUpgradeContainer.setChanged()
  → NodePluginManager resolves EtherStreamGrowthAcceleratorUpgrade

Node tick: FeatureEtherStreamEmitter.tickOutput()
  → creates EtherStreamEntity
  → iterates featureUpgradeStorage plugins
  → EtherStreamGrowthAcceleratorUpgrade.provideCapabilities(stream)
  → adds EtherStreamGrowthAcceleratorCapability to stream

Stream ticks (each server tick):
  1. capability.tick(stream)
     → block at stream.blockPos is CROP_ACCELERATABLE?
       → ether >= cost? → consumeEther(cost), state.randomTick()
  2. Movement + collision check
     → onHit() called for non-miss
     → blockState.is(ETHER_STREAM_PASS_THROUGH)? → return (pass through)
     → capability.shouldPassThrough(blockState)? → return (pass through)
     → else: normal collision → super.onHit() → onHitBlock() → ...
```

## Edge Cases

- **Stream out of ether during tick()**: `consumeEther()` deducts from stream's ether counter; if it reaches 0, next tick's base consumption check will trigger `dropAndDiscard()`.
- **Multiple upgrades installed**: Second Bone Meal in another upgrade slot → `getCapability()` finds existing → no-op (no stacking).
- **Node has both FunctionGrowthAccelerator and this upgrade**: They operate independently (node vs stream ether pools). No conflict.
- **Non-acceleratable block hit**: `shouldPassThrough()` returns false → normal collision behavior.
- **Client side**: `tick()` guards with `ServerLevel` check; `shouldPassThrough()` used in `onHit()` runs on both sides for sync but is safe (no side effects).
