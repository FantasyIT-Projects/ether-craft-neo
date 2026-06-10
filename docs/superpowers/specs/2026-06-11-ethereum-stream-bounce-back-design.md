# Ether Stream Bounce-Back & Recreate Design

**Date:** 2026-06-11
**Status:** Approved

## Overview

When an ether stream is destroyed with residual ether (>0), instead of triggering capability `onDestroy` and dropping items/releasing entities, a new reverse-moving ether stream is created that inherits all capabilities from the original. The original stream is then discarded without calling `onDestroy` on transferred capabilities.

## Goals

1. **Bounce-back**: Any destruction scenario (timeout, ether depletion, unhandled collision) with `ether > 0` creates a reverse stream.
2. **Generic `recreate`**: `IEtherStreamLike.recreate(Vec3 newMotion)` is a reusable method for stream recreation, not specific to bounce-back.
3. **Capability transfer**: Capabilities move (not copy) from old to new stream; old stream's capability list is cleared so `discard`/`markToRemove` won't trigger `onDestroy`.
4. **`onRecreate` callback**: Capabilities can react to being transferred via `IStreamCapability.onRecreate(IEtherStreamLike newStream)`.

## Design

### 1. `IEtherStreamLike` — new method

```java
/**
 * Recreates the ether stream with new motion, transferring all data and capabilities.
 * Old stream is marked dead (no onDestroy on caps). New stream is self-registered.
 */
IEtherStreamLike recreate(Vec3 newMotion);
```

### 2. `IStreamCapability` — new callback

```java
/**
 * Called after this capability has been transferred to a new stream via recreate().
 * Use for re-binding (e.g. re-binding consumer).
 */
default void onRecreate(IEtherStreamLike newStream) {}
```

### 3. Destruction logic change

Both `VirtualEtherStream.markDead()` and `EtherStreamEntity.dropAndDiscard()` add at the top:

```java
if (ether > 0) {
    recreate(deltaMovement().reverse());
    return;
}
```

`recreate()` internally sets `ether = 0` on the old stream and calls `markDead()` again. Since `ether == 0` and capabilities list is empty, no recursion occurs.

### 4. `recreate()` flow

```
recreate(newMotion):
  1. Create new stream with: ether, pos, newMotion, consumer, syncedData
  2. Move capabilities list: newStream.capabilities = oldStream.capabilities; oldStream.capabilities = new empty list
  3. Set consumer on each transferred cap (cap.setConsumer(newStream.consumer))
  4. Call cap.onRecreate(newStream) on each capability
  5. Register new stream (holder.add / world.addFreshEntity)
  6. oldStream.ether = 0; oldStream.markDead() (caps empty, ether=0 → no-op)
  7. Return newStream
```

### 5. `VirtualEtherStream` changes

- New field: `VirtualEtherStreamHolder holder` (injected at construction)
- Constructor: add `holder` parameter
- `fromData()`: add `holder` parameter
- Implement `recreate(Vec3 newMotion)` as above
- `markDead()`: add bounce-back guard at top

### 6. `VirtualEtherStreamHolder` changes

- Change foreach loops on `streams` to index-based to allow modification during iteration:
  - Line 74: `tick()` call
  - Line 112: collision loop
  - Line 190: ether glass check
- `createStream()`: pass `this` as holder to VirtualEtherStream
- `loadFromData()` / `fromData()`: pass `this` as holder

### 7. `EtherStreamEntity` changes

- Implement `recreate(Vec3 newMotion)`:
  1. Create new EtherStreamEntity via static factory
  2. Transfer capabilities (clear old list)
  3. Transfer consumer
  4. Call onRecreate on each cap
  5. `level.addFreshEntity(newEntity)`
  6. Old stream: `ether = 0; discard()` (caps empty, no onDestroy)
  7. Return newEntity
- `dropAndDiscard()`: add bounce-back guard at top

## Data Transfer Table

| Field | Handling |
|-------|----------|
| `ether` | Transferred (same amount) |
| `pos` | Transferred (same position) |
| `motion` | Set to `newMotion` parameter |
| `posDir` | Recalculated from new motion |
| `consumer` | Transferred (with state) |
| `capabilities` | **Moved** to new stream, old cleared |
| `syncedData` | Transferred |
| `tickCount` | Reset to 0 |
| `startPos` | Set to current pos |
| `markToRemove` | false (new stream) |
| `needsEtherSync` | Reset to false |

## Affected Files

| File | Change Type |
|------|-------------|
| `stream/IEtherStreamLike.java` | Add method |
| `stream/cap/IStreamCapability.java` | Add default method |
| `stream/vholder/VirtualEtherStream.java` | Add field, constructor param, implement recreate, modify markDead |
| `stream/vholder/VirtualEtherStreamHolder.java` | foreach→index, pass holder to VES |
| `entity/stream/EtherStreamEntity.java` | Implement recreate, modify dropAndDiscard |

## Non-Goals

- No additional ether cost for bounce-back
- No bounce count limit
- `onRecreate` is a default no-op; individual capabilities opt in
