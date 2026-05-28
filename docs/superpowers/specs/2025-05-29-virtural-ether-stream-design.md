# Virtual Ether Stream — Design Spec

## Overview

Replace the existing `EtherStreamEntity` (Minecraft `Projectile` entity) with a purely server-side virtual stream system backed by custom network sync and client-side rendering via `SubmitCustomGeometryEvent`.

The old system spawns one real `Entity` per ether stream, relying on Minecraft's built-in entity sync (`setUpdateInterval(1)`) and `ProjectileUtil` hit detection. The new system eliminates entity overhead per stream by moving all stream logic to server-side POJOs, syncing only keyframe data via payload packets, and rendering on the client using cached data and extrapolation.

**Transition strategy**: Keep existing `EtherStreamEntity` / `EtherStreamEntityRenderer` code and mark `@Deprecated`. Remove after virtual stream is stable.

---

## Architecture

```
ServerLevel
  └── VESHM (level attachment, Map<PosDir, VESH>)
        ├── PosDir(emitterPos, NORTH) → VESH
        │     ├── direction: NORTH
        │     ├── activateTick: N (countdown, removed when 0 and no VES)
        │     └── List<VirtualEtherStream>
        ├── PosDir(emitterPos, SOUTH) → VESH
        │     └── ...
        └── ...

ServerLevel tick → VESHM.tick()
  ├── cache.beforeTick()
  ├── for each VESH:
  │     ├── VESH.tick() → advance each VES, collision, consume, cleanup
  │     └── activateTick--; remove if empty and expired
  └── send sync payloads to tracking clients

Emitter BE (tickServer):
  └── VESHM.createStream(level, posDir, ether, pos, motion) → IEtherStreamLike
  └── attach capabilities to IEtherStreamLike
  └── no longer holds VESHM reference

Client:
  ClientVESHData (level attachment)
    └── Receive sync payloads → update stream entries
    └── SubmitCustomGeometryEvent → render from cached data with extrapolation
```

### Key Entities

| Class | Side | Responsibility |
|---|---|---|
| `VESHM` | Server | Level attachment, `Map<PosDir, VESH>`, tick entry point, `createStream()` |
| `VESH` | Server | Per-direction, manages `List<VirtualEtherStream>`, tick/cleanup |
| `VirtualEtherStream` | Server | Single stream, carries position/motion/ether/capabilities, implements `IEtherStreamLike` |
| `ClientVESHData` | Client | Level attachment, caches synced VES data for rendering |
| `ChainedEmitterEntityHitCache` | Server | Per-tick entity cache, used by VESH for efficient collision queries |

---

## Data Model

### VirtualEtherStream (enhanced)

New fields and methods added to the existing class:

```
// Existing
Vec3 pos, Level level, Direction direction, int ether, List<IStreamCapability> capabilities

// New fields
Vec3 startPos          // initial position (client extrapolation origin)
Vec3 motion            // velocity vector (client extrapolation)
int tickCount          // ticks alive (decay + max lifetime)
boolean dead           // marked for removal
boolean dying          // in death animation (requires label)
int deathTick          // death animation tick counter (0 if not dying)
int labelColor         // ARGB label color
@Nullable Component label  // label text

// New methods
void markDead()        // sets dead=true without triggering onDestroy (onDestroy called later by VESH)
void doCollision(ChainedEmitterEntityHitCache cache, PosDir posDir, float motionLen)
int getConsumption()   // same formula as EtherStreamEntity.getConsumption()
```

### VESH (rewrite of VirtualEtherStreamHolder)

No longer extends `Entity`. Pure server-side POJO:

```
Direction direction          // stream direction
int activateTick = 5         // managed by VESHM, counts down each tick. Default 5 (same as old VirtualEtherStreamHolder)
List<VirtualEtherStream> streams

void tick(ChainedEmitterEntityHitCache cache)
void createStream(int ether, Vec3 pos, Vec3 motion) → VirtualEtherStream
boolean isDead()              // true when activateTick<=0 and streams.isEmpty()
```

### VESHM (new)

Level attachment:

```
Map<PosDir, VESH> holders

void tick(ServerLevel level)
IEtherStreamLike createStream(Level level, PosDir posDir, int ether, Vec3 pos, Vec3 motion)
```

### ClientVESHData (new)

Client-side level attachment:

```
Map<PosDir, ClientVESHEntry> entries

ClientVESHEntry:
  Direction direction
  Map<Integer, ClientStreamEntry> streams

ClientStreamEntry:
  Vec3 startPos, Vec3 motion
  int startTickCount, int ether
  long receivedAtTick (client tick)
  byte flags (DEAD=1, DYING=2)
  int deathTick
  @Nullable Component label, int labelColor
```

---

## Sync Protocol

Keyframe-based sync. Two payload types, both S2C.

### EtherStreamCreateS2C

Sent when a VES is first created.

```
PosDir posDir
int streamId
Vec3 startPos
Vec3 motion
int ether
int tickCount
@Nullable Component label (optional)
int labelColor
```

### EtherStreamUpdateS2C

Sent periodically (every tick or every N ticks) to batch-update all VES for a PosDir. Contains create, update, and remove information.

```
PosDir posDir
List<StreamEntry>:
  int streamId
  int tickCount
  int ether
  byte flags          (bit0=dead, bit1=dying)
  int deathTick
  @Nullable Component label
  int labelColor
```

Distribution: `PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunkPos, payload)` using the emitter's chunk position.

Client handles each entry in the update batch:
- streamId not in cache → treat as create (add new ClientStreamEntry)
- flags & DEAD and !DYING → remove immediately from cache
- flags & DYING → update death state, keep in cache
- otherwise → update tickCount/ether/label for extrapolation alignment

Removal of fully-expired streams (neither dead nor dying, just gone): a VES that has been completely removed from the server is simply absent from the update batch. After processing all entries in the batch, any streamId in the client cache that was not present in this update is removed — it has expired without fanfare (no label, instant discard).

---

## Tick Logic

### VESHM.tick()

```
cache.beforeTick()
List<PosDir> toRemove

for (posDir, vesh) in holders:
    vesh.tick(cache)
    vesh.activateTick--
    if vesh.isDead():
        toRemove.add(posDir)

for posDir in toRemove: holders.remove(posDir)

sendSyncPayloads(level)
```

### VESH.tick(cache)

```
for ves in streams (filtered copy, allow concurrent modification):
    ves.tickCount++

    // Max lifetime
    if ves.tickCount > Config.etherStreamMaxTick:
        ves.markDead()

    if !ves.dead:
        // Ether consumption
        consumption = getConsumption(ves)  // reuse existing formula from EtherStreamEntity
        ves.ether -= consumption
        if ves.ether <= 0:
            ves.markDead()

    // Capability tick (always, even dead — caps may need cleanup)
    for cap in ves.capabilities:
        cap.tick(ves)
    if ves.ether <= 0:
        ves.markDead()

    if !ves.dead:
        // Collision detection
        ves.doCollision(cache)
        // Move
        ves.pos = ves.pos.add(ves.motion)

    // Death animation
    if ves.dead && !ves.dying:
        if ves.label != null:
            ves.dying = true
            ves.deathTick = 0
            for cap in ves.capabilities: cap.onDestroy(ves)
        else:
            ves.deathTick = -1  // mark for immediate removal
    if ves.dying:
        ves.deathTick++
        ves.pos = ves.pos.add(ves.motion)
        if ves.deathTick > 60:
            ves.deathTick = -1  // mark for removal

// Cleanup
streams.removeIf(ves -> ves.deathTick == -1)
```

### Collision Detection (VirtualEtherStream.doCollision)

Signature: `doCollision(ChainedEmitterEntityHitCache cache, PosDir posDir, float motionLen)`

Where `posDir` is the VESH's PosDir (emitter position + direction) and `motionLen` is `|ves.motion|`.

```
// Entity hits via cache
entities = cache.getAllEntities(ves.pos, posDir, 0, motionLen)
for entity in entities:
    if entity.hitbox intersects (oldPos → newPos):
        handled = false
        for cap in capabilities: handled |= cap.hitEntity(entity)
        if !handled: markDead()

// Block hit via position lookup
newBlockPos = BlockPos.containing(ves.pos.add(ves.motion))
block = level.getBlockState(newBlockPos)
for cap in capabilities:
    if cap.shouldPassThrough(block): return  // skip collision
handled = false
for cap in capabilities: handled |= cap.hitBlock(block)
if !handled: markDead()
```

### ChainedEmitterEntityHitCache changes

Add overload that accepts `Vec3` instead of `Entity source`:

```java
public @Nullable List<Entity> getAllEntities(Vec3 pos, PosDir posDir, float backwardDist, float forwardDist)
```

`getAllEntitiesRect` still uses the `Entity source` (exclusion parameter) for the `level.getEntities()` call. For the Vec3 overload, pass `null` as source (no exclusion needed for virtual streams).

ChainedEmitterEntityHitCache is held by VESHM and passed into VESH.tick(). `beforeTick()` is called once at the start of VESHM.tick().

---

## Rendering

### ClientVESHData

Stored as a client-side level attachment. Updated via `EtherStreamCreateS2C` and `EtherStreamUpdateS2C` handlers.

### EtherStreamRenderEvent

`@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)` subscribing to `SubmitCustomGeometryEvent`.

```
onRender(SubmitCustomGeometryEvent event):
    data = Minecraft.level.getData(CLIENT_VESH_DATA)
    if data == null: return

    // Iterate sync-only copies to avoid CME
    for (posDir, entry) in data.snapshot():
        for (streamId, stream) in entry.streams:
            if stream.isRemoved(): continue

            // Extrapolate position
            clientTick = Minecraft.level.getGameTime()
            elapsed = clientTick - stream.receivedAtTick
            currentPos = stream.startPos + stream.motion * (stream.startTickCount + elapsed + partialTick)

            // Tail: uniform backward points (no ring buffer)
            // tailSpacingFactor = |motion| (1 tick displacement, same visual spacing as entity sampler)
            // baseSize = 1.0 (same as current entity tail base)
            for i = 0..5:
                tailPos = currentPos - stream.motion.normalize() * i * |motion|
                alpha = 1.0 - i / 6.1f
                size = 1.0f / 1.5f^i
                submit billboard quad at tailPos with alpha and size

            // Label
            if stream.label != null:
                compute visible text (left-clip by startPos distance, right-clip by deathTick)
                compute normal from motion
                collector.submitText(...) on both faces
```

Rendering reuses the existing `RENDER_TYPE` pipeline (`ETHER_STREAM_ENTITY_PIPELINE` + `ether_stream.png` texture) from `EtherStreamEntityRenderer`.

### Death Animation

Same as current `EtherStreamEntityRenderer.renderLabel()`:
- Dying streams: label text consumed from right as `deathTick * speed / LABEL_SCALE`
- Alive streams: label text clipped from left at `startPos` distance
- Position: anchored at deathPos for dying, at currentPos for alive

---

## File Changes

### New Files

| File | Description |
|---|---|
| `entity/vholder/VESHM.java` | Level attachment, Map<PosDir, VESH>, tick, createStream |
| `entity/vholder/VESH.java` | Rewrite of VirtualEtherStreamHolder, no Entity parent, per-direction VES manager |
| `network/s2c/EtherStreamCreateS2C.java` | Create sync payload |
| `network/s2c/EtherStreamUpdateS2C.java` | Batch update sync payload |
| `event/ClientVESHData.java` | Client level attachment, VES data cache |
| `event/EtherStreamRenderEvent.java` | SubmitCustomGeometryEvent subscriber, renders virtual streams |

### Modified Files

| File | Changes |
|---|---|
| `VirtualEtherStream.java` | Add startPos, motion, tickCount, dead/dying flags, deathTick, label, labelColor, markDead(), doCollision(), getConsumption() |
| `VirtualEtherStreamHolder.java` → `VESH.java` | Remove Entity inheritance, rewrite as server-side POJO |
| `ChainedEmitterEntityHitCache.java` | Add `getAllEntities(Vec3 pos, ...)` overload; document that `beforeTick()` must be called |
| `EtherStreamEmitterEntity.java` | Replace `EtherStreamEntity.create() + addFreshEntity()` with `VESHM.createStream()` |
| `FeatureEtherStreamEmitter.java` | Same replacement as above |
| `IEtherStreamCapabilityProviderPlugin.java` | Change parameter from `EtherStreamEntity` to `IEtherStreamLike` |
| `EtherStreamTextUpgrade.java` + other upgrades | Adjust to use `IEtherStreamLike` instead of `EtherStreamEntity` |
| `Network.java` | Register two new S2C payload types |
| `AttachmentDataRegistry.java` | Register VESHM and ClientVESHData attachment types |
| `EtherStreamEntity.java` | Mark `@Deprecated` |
| `EtherStreamEntityRenderer.java` | Mark `@Deprecated` |
| `EntityRegistry.java` | Mark ETHER_STREAM_ENTITY registration `@Deprecated` (keep for transitional compat) |

---

## Behavior Parity Checklist

- [ ] Stream creation at emitter position + direction offset
- [ ] Per-tick ether consumption (same formula: `factor * ether + sum(cap.getConsumption())`)
- [ ] Capability tick loop (storage pickup, growth acceleration, block breaking)
- [ ] Block collision with pass-through tags (`Tags.ETHER_STREAM_PASS_THROUGH`)
- [ ] Entity collision (damage, item transfer)
- [ ] Max lifetime (Config.etherStreamMaxTick)
- [ ] Death animation with labels (60 tick dying state, text consumption from right)
- [ ] Immediate discard without labels
- [ ] Tail rendering (6 quads, alpha fade, size reduction)
- [ ] Label rendering (left-clip alive, right-clip dying, both faces)
- [ ] Item storage capability (pickup ItemEntity, transfer to player/block, drop on destroy)
- [ ] Item filter via FeatureEtherStreamEmitter
- [ ] Efficient entity collision via ChainedEmitterEntityHitCache
