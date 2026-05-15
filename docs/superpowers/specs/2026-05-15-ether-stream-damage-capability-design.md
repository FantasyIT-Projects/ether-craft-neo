# Ether Stream Damage Capability Design

## Summary

Add a new `IStreamCapability` (`EtherStreamDamageCapability`) that deals damage to entities on hit, mirroring the existing `EtherStreamBreakBlockCapability` pattern. Weapons are validated via `DataComponents.WEAPON`.

## Files

| Action | File |
|--------|------|
| CREATE | `stream/EtherStreamDamageCapability.java` |
| CREATE | `node/plugins/upgrade/EtherStreamDamageUpgrade.java` |
| MODIFY | `node/NodePluginManager.java` — register plugin with WEAPON predicate |
| MODIFY | `Config.java` — add `damage.ether_multiplier` config |

## Design Details

### EtherStreamDamageCapability
- Stores `List<ItemStack> weapons`
- `getId()` → `ether_craft:damage_dealer`
- `getConsumption()` → 0 (no passive cost, like BreakBlock)
- `hitEntity()` → find best weapon by `itemDamagePerAttack`, deal `hurt()`, consume ether = `damage * multiplier`
- `hitBlock()` → false (no-op)
- Serialization: `ItemStack.OPTIONAL_CODEC.listOf()` for "weapons"

### EtherStreamDamageUpgrade
- Implements `IEtherStreamCapabilityProviderPlugin`
- Gets weapon from node's upgrade slot
- Provides/extends `EtherStreamDamageCapability` on stream entity

### Registration
- Plugin type: `UPGRADE`
- Predicate: `stack -> stack.has(DataComponents.WEAPON)`
- Icon: `Items.IRON_SWORD`

### Config
- `damage.ether_multiplier` (default 5) — ether consumed per point of damage dealt
