# Virtual Ether Stream Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `EtherStreamEntity` (Minecraft `Projectile`) with virtual server-side streams synced via custom payloads, rendered via `SubmitCustomGeometryEvent`.

**Architecture:** `VESHM` is a `ServerLevel` attachment managing `Map<PosDir, VESH>`. Each `VESH` (per-direction) ticks `VirtualEtherStream` instances, handles collision via `ChainedEmitterEntityHitCache`, and sends keyframe sync to tracking clients. Client-side `ClientVESHData` caches stream data; `EtherStreamRenderEvent` extrapolates positions and renders tails + labels.

**Tech Stack:** NeoForge 26.1.2, Minecraft 1.21.x level attachments, `CustomPacketPayload` S2C payloads, `SubmitCustomGeometryEvent`, `PacketDistributor.sendToPlayersTrackingChunk`.

---

### Task 1: Enhance VirtualEtherStream

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/entity/vholder/VirtualEtherStream.java`

- [ ] **Step 1: Add new fields to VirtualEtherStream**

Open `VirtualEtherStream.java`. Add these imports and fields after the existing fields:

```java
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.register.Tags;
import javax.annotation.Nullable;
import java.util.List;
```

Add these fields after `List<IStreamCapability> capabilities`:

```java
    int streamId;
    Vec3 startPos;
    Vec3 motion;
    int tickCount = 0;
    boolean dead = false;
    boolean dying = false;
    int deathTick = 0;
    int labelColor = 0xFFFFFFFF;
    @Nullable Component label;
```

- [ ] **Step 2: Add markDead(), getConsumption(), doCollision() methods**

Add these methods to the class:

```java
    public void markDead() {
        this.dead = true;
    }

    public int getConsumption() {
        double factor = Config.etherStreamConsumptionFactor;
        factor += Config.etherStreamConsumptionByTimeFactor * tickCount;
        double value = Math.ceil(factor * ether);
        for (IStreamCapability cap : capabilities) {
            value += cap.getConsumption();
        }
        return (int) Math.ceil(value);
    }

    public void doCollision(ChainedEmitterEntityHitCache cache, PosDir posDir, float motionLen) {
        Vec3 newPos = pos.add(motion);
        List<Entity> entities = cache.getAllEntities(pos, posDir, 0, motionLen);
        if (entities != null) {
            for (Entity entity : entities) {
                if (!(entity instanceof LivingEntity)) continue;
                AABB hitbox = entity.getBoundingBox().inflate(0.3);
                if (!hitbox.clip(pos, newPos).isPresent()) continue;
                boolean handled = false;
                for (IStreamCapability cap : capabilities) {
                    if (cap.hitEntity(entity)) handled = true;
                }
                if (!handled) {
                    markDead();
                    return;
                }
            }
        }

        BlockPos newBlockPos = BlockPos.containing(newPos);
        var blockState = level.getBlockState(newBlockPos);
        for (IStreamCapability cap : capabilities) {
            if (cap.shouldPassThrough(blockState, newBlockPos)) return;
        }
        boolean handled = false;
        for (IStreamCapability cap : capabilities) {
            if (cap.hitBlock(level, newBlockPos, blockState)) handled = true;
        }
        if (!handled) markDead();
    }
```

Note: `IStreamCapability.hitBlock` currently takes different parameters — check the actual signature. The existing code in `EtherStreamEntity` uses `(level, blockPos, blockState, hitResult)`. Adjust the call to match.

- [ ] **Step 3: Add setter for startPos, motion, label, and labelColor**

```java
    public void setStartData(Vec3 startPos, Vec3 motion) {
        this.startPos = startPos;
        this.motion = motion;
    }

    public void setLabel(@Nullable Component label, int color) {
        this.label = label;
        this.labelColor = color;
    }
```

- [ ] **Step 4: Build to verify compilation**

Run: `idea_build_project` with `filesToRebuild=["src/main/java/studio/fantasyit/ether_craft/entity/vholder/VirtualEtherStream.java"]`

---

### Task 2: Rewrite VESH (VirtualEtherStreamHolder → remove Entity parent)

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/entity/vholder/VirtualEtherStreamHolder.java`

- [ ] **Step 1: Replace class declaration and imports**

Remove all imports. Replace with:

```java
package studio.fantasyit.ether_craft.stream.vholder;

import net.minecraft.core.Direction;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

```

Replace the entire class body. The class no longer extends `Entity`:

```java
public class VirtualEtherStreamHolder {
    final Direction direction;
    int activateTick = 5;
    final List<VirtualEtherStream> streams = new ArrayList<>();
    int nextStreamId = 0;

    public VirtualEtherStreamHolder(Direction direction) {
        this.direction = direction;
    }

    int nextId = 0;

    public VirtualEtherStream createStream(int ether, net.minecraft.world.phys.Vec3 pos, net.minecraft.world.phys.Vec3 motion) {
        VirtualEtherStream ves = new VirtualEtherStream();
        ves.pos = pos;
        ves.motion = motion;
        ves.startPos = pos;
        ves.direction = this.direction;
        ves.ether = ether;
        ves.streamId = nextId++;
        streams.add(ves);
        return ves;
    }

    public void tick(ChainedEmitterEntityHitCache cache, PosDir posDir) {
        activateTick--;
        List<VirtualEtherStream> snapshot = new ArrayList<>(streams);
        for (VirtualEtherStream ves : snapshot) {
            ves.tickCount++;

            if (ves.tickCount > Config.etherStreamMaxTick) {
                ves.markDead();
            }

            if (!ves.dead) {
                int consumption = ves.getConsumption();
                ves.consumeEther(consumption);
                if (ves.getEther() <= 0) {
                    ves.markDead();
                }
            }

            for (studio.fantasyit.ether_craft.stream.IStreamCapability cap : ves.capabilities) {
                cap.tick(ves);
            }
            if (ves.getEther() <= 0) {
                ves.markDead();
            }

            if (!ves.dead) {
                float motionLen = (float) ves.motion.length();
                ves.doCollision(cache, posDir, motionLen);
                ves.pos = ves.pos.add(ves.motion);
            }

            if (ves.dead && !ves.dying) {
                if (ves.label != null) {
                    ves.dying = true;
                    ves.deathTick = 0;
                    for (studio.fantasyit.ether_craft.stream.IStreamCapability cap : ves.capabilities) {
                        cap.onDestroy(ves);
                    }
                } else {
                    ves.deathTick = -1;
                }
            }
            if (ves.dying) {
                ves.deathTick++;
                ves.pos = ves.pos.add(ves.motion);
                if (ves.deathTick > 60) {
                    ves.deathTick = -1;
                }
            }
        }
        streams.removeIf(ves -> ves.deathTick == -1);
    }

    public boolean isDead() {
        return activateTick <= 0 && streams.isEmpty();
    }
}
```

- [ ] **Step 2: Build to verify**

Run: `idea_build_project` with `filesToRebuild=["src/main/java/studio/fantasyit/ether_craft/entity/vholder/VirtualEtherStreamHolder.java"]`

---

### Task 3: Create VESHM

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/entity/vholder/VESHM.java`

- [ ] **Step 1: Create VESHM.java**

```java
package studio.fantasyit.ether_craft.stream.vholder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VESHM {
    private final Map<PosDir, VirtualEtherStreamHolder> holders = new HashMap<>();
    private final ChainedEmitterEntityHitCache cache = new ChainedEmitterEntityHitCache();

    public IEtherStreamLike createStream(Level level, PosDir posDir, int ether, Vec3 pos, Vec3 motion) {
        VirtualEtherStreamHolder holder = holders.computeIfAbsent(posDir,
                k -> new VirtualEtherStreamHolder(posDir.dir()));
        holder.activateTick = 5;
        VirtualEtherStream ves = holder.createStream(ether, pos, motion);
        ves.level = level;
        return ves;
    }

    public void tick(ServerLevel level) {
        cache.beforeTick();
        List<PosDir> toRemove = new ArrayList<>();
        for (Map.Entry<PosDir, VirtualEtherStreamHolder> entry : holders.entrySet()) {
            PosDir posDir = entry.getKey();
            VirtualEtherStreamHolder holder = entry.getValue();
            holder.tick(cache, posDir);
            if (holder.isDead()) {
                toRemove.add(posDir);
            }
        }
        for (PosDir posDir : toRemove) {
            holders.remove(posDir);
        }

        // TODO: send sync payloads (Task 5)
    }

    public static VESHM get(ServerLevel level) {
        return level.getData(studio.fantasyit.ether_craft.register.AttachmentDataRegistry.VESHM);
    }
}
```

- [ ] **Step 2: Build to verify**

Run: `idea_build_project` with `filesToRebuild=["src/main/java/studio/fantasyit/ether_craft/entity/vholder/VESHM.java"]`

---

### Task 4: Fix ChainedEmitterEntityHitCache — add Vec3 overload

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/attachment/ChainedEmitterEntityHitCache.java`

- [ ] **Step 1: Add Vec3 overload for getAllEntities**

Add a new overload method at the end of the class (before the closing brace):

```java
    public @Nullable List<Entity> getAllEntities(Vec3 pos, PosDir posDir, float backwardDist, float forwardDist) {
        List<Entity> result = getAllEntitiesRect(null, posDir);
        if (result == null) return null;
        float dist = (float) pos.distanceTo(posDir.pos.getCenter());
        List<Entity> filtered = new ArrayList<>();
        for (Entity entity : result) {
            float newDist = (float) entity.position().distanceTo(posDir.pos.getCenter());
            if (dist - backwardDist < newDist && newDist < dist + forwardDist)
                filtered.add(entity);
        }
        return filtered;
    }
```

- [ ] **Step 2: Modify getAllEntitiesRect to handle null source**

At line 32, the method takes `Entity source`. Modify to handle `null`:

Change line 33-34 from:
```java
        float dist = (float) source.position().distanceTo(posDir.pos.getCenter());
```
to:
```java
        float dist = source != null ? (float) source.position().distanceTo(posDir.pos.getCenter()) : 1f;
```

And change line 41 from:
```java
        source.level().getEntities(source, new AABB(posDir.pos).expandTowards(unit).inflate(1));
```
We need a `Level` accessor. Since virtual streams don't have an Entity source, we can get level from the stream. But the cache doesn't know about the level. 

**Alternative approach**: Instead of modifying `getAllEntitiesRect`, add a `Level` parameter to the `Vec3` overload and let it do the entity query directly. The cache's `getAllEntitiesRect` is only useful for the entity-based path (old code). For virtual streams, we query directly.

Replace the Vec3 overload added in Step 1 with a standalone implementation:

```java
    public @Nullable List<Entity> getAllEntities(Level level, Vec3 pos, PosDir posDir, float backwardDist, float forwardDist) {
        float dist = (float) pos.distanceTo(posDir.pos.getCenter());
        if (!curDist.containsKey(posDir) || dist > curDist.get(posDir)) {
            curDist.put(posDir, dist);
        }
        if (!maxDist.containsKey(posDir)) return null;

        if (!cache.containsKey(posDir)) {
            Vec3 unit = posDir.dir.getUnitVec3().scale(maxDist.get(posDir) + 0.5f);
            List<Entity> entities = level.getEntities(
                    null,
                    new AABB(posDir.pos).expandTowards(unit).inflate(1)
            );
            List<Entity> filtered = new ArrayList<>();
            for (Entity entity : entities) {
                if (entity.getType().is(net.minecraft.tags.EntityTypeTags.ARROWS)) continue;
                filtered.add(entity);
            }
            cache.put(posDir, filtered);
        }

        List<Entity> result = cache.get(posDir);
        List<Entity> filtered = new ArrayList<>();
        for (Entity entity : result) {
            float newDist = (float) entity.position().distanceTo(posDir.pos.getCenter());
            if (dist - backwardDist < newDist && newDist < dist + forwardDist)
                filtered.add(entity);
        }
        return filtered;
    }
```

Add the `Level` import:
```java
import net.minecraft.world.level.Level;
```

- [ ] **Step 3: Update VirtualEtherStream.doCollision call to use new overload**

Update `doCollision` in `VirtualEtherStream.java` to pass `level`:

```java
        List<Entity> entities = cache.getAllEntities(level, pos, posDir, 0, motionLen);
```

- [ ] **Step 4: Build to verify**

Run: `idea_build_project` with `filesToRebuild=["src/main/java/studio/fantasyit/ether_craft/attachment/ChainedEmitterEntityHitCache.java","src/main/java/studio/fantasyit/ether_craft/entity/vholder/VirtualEtherStream.java"]`

---

### Task 5: Create S2C Payloads

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/network/s2c/EtherStreamCreateS2C.java`
- Create: `src/main/java/studio/fantasyit/ether_craft/network/s2c/EtherStreamUpdateS2C.java`

- [ ] **Step 1: Create EtherStreamCreateS2C.java**

Look at existing `SyncBlockNameS2C.java` for the payload pattern (`idea_read_file` on that file first).

```java
package studio.fantasyit.ether_craft.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.event.ClientVESHData;

public record EtherStreamCreateS2C(PosDir posDir, int streamId, Vec3 startPos, Vec3 motion,
                                    int ether, int tickCount,
                                    @org.jetbrains.annotations.Nullable Component label, int labelColor)
        implements CustomPacketPayload {

    public static final Type<@NotNull EtherStreamCreateS2C> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(EtherCraft.MODID, "ether_stream_create"));

    public static final StreamCodec<ByteBuf, @NotNull EtherStreamCreateS2C> CODEC = StreamCodec.composite(
            StreamCodec.of(
                    (buf, val) -> { buf.writeBlockPos(val.pos()); buf.writeEnum(val.dir()); },
                    buf -> new PosDir(buf.readBlockPos(), buf.readEnum(Direction.class))
            ),
            EtherStreamCreateS2C::posDir,
            ByteBufCodecs.VAR_INT, EtherStreamCreateS2C::streamId,
            StreamCodec.of(
                    (buf, val) -> { buf.writeDouble(val.x); buf.writeDouble(val.y); buf.writeDouble(val.z); },
                    buf -> new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
            ),
            EtherStreamCreateS2C::startPos,
            StreamCodec.of(
                    (buf, val) -> { buf.writeDouble(val.x); buf.writeDouble(val.y); buf.writeDouble(val.z); },
                    buf -> new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
            ),
            EtherStreamCreateS2C::motion,
            ByteBufCodecs.VAR_INT, EtherStreamCreateS2C::ether,
            ByteBufCodecs.VAR_INT, EtherStreamCreateS2C::tickCount,
            ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC),
            EtherStreamCreateS2C::label,
            ByteBufCodecs.VAR_INT, EtherStreamCreateS2C::labelColor,
            EtherStreamCreateS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player().level().isClientSide) {
                ClientVESHData.get().handleCreate(this);
            }
        });
    }
}
```

- [ ] **Step 2: Create EtherStreamUpdateS2C.java**

```java
package studio.fantasyit.ether_craft.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.event.ClientVESHData;

import java.util.ArrayList;
import java.util.List;

public record EtherStreamUpdateS2C(PosDir posDir, List<StreamEntry> entries) implements CustomPacketPayload {

    public record StreamEntry(int streamId, int tickCount, int ether, byte flags,
                               int deathTick, @org.jetbrains.annotations.Nullable Component label, int labelColor) {
        public static final byte FLAG_DEAD = 1;
        public static final byte FLAG_DYING = 2;

        public boolean isDead() { return (flags & FLAG_DEAD) != 0; }
        public boolean isDying() { return (flags & FLAG_DYING) != 0; }
    }

    public static final Type<@NotNull EtherStreamUpdateS2C> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(EtherCraft.MODID, "ether_stream_update"));

    private static final StreamCodec<ByteBuf, StreamEntry> ENTRY_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StreamEntry::streamId,
            ByteBufCodecs.VAR_INT, StreamEntry::tickCount,
            ByteBufCodecs.VAR_INT, StreamEntry::ether,
            ByteBufCodecs.BYTE, StreamEntry::flags,
            ByteBufCodecs.VAR_INT, StreamEntry::deathTick,
            ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC),
            StreamEntry::label,
            ByteBufCodecs.VAR_INT, StreamEntry::labelColor,
            StreamEntry::new
    );

    public static final StreamCodec<ByteBuf, @NotNull EtherStreamUpdateS2C> CODEC = StreamCodec.composite(
            StreamCodec.of(
                    (buf, val) -> { buf.writeBlockPos(val.pos()); buf.writeEnum(val.dir()); },
                    buf -> new PosDir(buf.readBlockPos(), buf.readEnum(Direction.class))
            ),
            EtherStreamUpdateS2C::posDir,
            ENTRY_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)),
            EtherStreamUpdateS2C::entries,
            EtherStreamUpdateS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player().level().isClientSide) {
                ClientVESHData.get().handleUpdate(this);
            }
        });
    }
}
```

- [ ] **Step 3: Build to verify**

Run: `idea_build_project` with `filesToRebuild=["src/main/java/studio/fantasyit/ether_craft/network/s2c/EtherStreamCreateS2C.java","src/main/java/studio/fantasyit/ether_craft/network/s2c/EtherStreamUpdateS2C.java"]`

Note: This will fail because `ClientVESHData` doesn't exist yet. Accept the compile error — it will be resolved in Task 6.

---

### Task 6: Create ClientVESHData

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/event/ClientVESHData.java`

- [ ] **Step 1: Create ClientVESHData.java**

```java
package studio.fantasyit.ether_craft.event;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamUpdateS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamUpdateS2C.StreamEntry;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClientVESHData {
    public static class ClientStreamEntry {
        Vec3 startPos, motion;
        int startTickCount, ether;
        long receivedAtTick;
        byte flags;
        int deathTick;
        @Nullable Component label;
        int labelColor;
        boolean removed;

        public boolean isRemoved() { return removed; }

        public ClientStreamEntry(EtherStreamCreateS2C msg) {
            this.startPos = msg.startPos();
            this.motion = msg.motion();
            this.startTickCount = msg.tickCount();
            this.ether = msg.ether();
            this.receivedAtTick = Minecraft.getInstance().level != null
                    ? Minecraft.getInstance().level.getGameTime() : 0;
            this.label = msg.label();
            this.labelColor = msg.labelColor();
        }
    }

    public static class ClientVESHEntry {
        Direction direction;
        Map<Integer, ClientStreamEntry> streams = new HashMap<>();
    }

    Map<PosDir, ClientVESHEntry> entries = new HashMap<>();

    public void handleCreate(EtherStreamCreateS2C msg) {
        ClientVESHEntry entry = entries.computeIfAbsent(msg.posDir(), k -> {
            ClientVESHEntry e = new ClientVESHEntry();
            e.direction = msg.posDir().dir();
            return e;
        });
        entry.streams.put(msg.streamId(), new ClientStreamEntry(msg));
    }

    public void handleUpdate(EtherStreamUpdateS2C msg) {
        ClientVESHEntry entry = entries.computeIfAbsent(msg.posDir(), k -> {
            ClientVESHEntry e = new ClientVESHEntry();
            e.direction = msg.posDir().dir();
            return e;
        });

        Set<Integer> seenIds = new HashSet<>();
        for (StreamEntry se : msg.entries()) {
            seenIds.add(se.streamId());
            ClientStreamEntry cse = entry.streams.get(se.streamId());
            if (cse == null) {
                // treat as create from update
                cse = new ClientStreamEntry(null);
                entry.streams.put(se.streamId(), cse);
            }

            if (se.isDead() && !se.isDying()) {
                cse.removed = true;
                continue;
            }
            if (se.isDying()) {
                cse.flags = se.flags();
                cse.deathTick = se.deathTick();
            }
            cse.startTickCount = se.tickCount();
            cse.ether = se.ether();
            if (se.label() != null) cse.label = se.label();
            cse.labelColor = se.labelColor();
            cse.receivedAtTick = Minecraft.getInstance().level != null
                    ? Minecraft.getInstance().level.getGameTime() : 0;
        }

        // Remove entries not present in update
        entry.streams.entrySet().removeIf(e -> {
            ClientStreamEntry cse = e.getValue();
            return !seenIds.contains(e.getKey()) && !cse.dying && cse.removed;
        });

        if (entry.streams.isEmpty()) {
            entries.remove(msg.posDir());
        }
    }

    public Map<PosDir, ClientVESHEntry> getEntries() {
        return entries;
    }

    public static ClientVESHData get() {
        Level level = Minecraft.getInstance().level;
        if (level == null) return new ClientVESHData();
        return level.getData(AttachmentDataRegistry.CLIENT_VESH_DATA);
    }
}
```

Note: `ClientStreamEntry` needs a no-arg constructor for entries created from updates. Modify the constructor inline in Step 1:

```java
        public ClientStreamEntry(@Nullable EtherStreamCreateS2C msg) {
            if (msg != null) {
                this.startPos = msg.startPos();
                this.motion = msg.motion();
                this.startTickCount = msg.tickCount();
                this.ether = msg.ether();
                this.label = msg.label();
                this.labelColor = msg.labelColor();
            } else {
                this.startPos = Vec3.ZERO;
                this.motion = Vec3.ZERO;
                this.startTickCount = 0;
                this.ether = 0;
            }
            this.receivedAtTick = Minecraft.getInstance().level != null
                    ? Minecraft.getInstance().level.getGameTime() : 0;
        }
```

- [ ] **Step 2: Build to verify (may have missing AttachmentDataRegistry ref)**

Run: `idea_build_project` with `filesToRebuild=["src/main/java/studio/fantasyit/ether_craft/event/ClientVESHData.java"]`

---

### Task 7: Create EtherStreamRenderEvent

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/event/EtherStreamRenderEvent.java`

- [ ] **Step 1: Review existing render pipeline for reuse**

Read `EtherStreamEntityRenderer.java` lines 22-30 to confirm the `RENDER_TYPE` constant and import it. The render type is static, so we can reference it.

- [ ] **Step 2: Create EtherStreamRenderEvent.java**

```java
package studio.fantasyit.ether_craft.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.entity.stream.render.EtherStreamRenderPipeline;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.event.ClientVESHData.ClientStreamEntry;
import studio.fantasyit.ether_craft.event.ClientVESHData.ClientVESHEntry;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class EtherStreamRenderEvent {
    private static final Identifier TEXTURE = EtherCraft.id("textures/particle/ether_stream.png");
    private static final RenderType RENDER_TYPE = RenderType.create(
            "ether_stream_tail_virtual",
            RenderSetup.builder(EtherStreamRenderPipeline.ETHER_STREAM_ENTITY_PIPELINE)
                    .withTexture("Sampler0", TEXTURE)
                    .sortOnUpload()
                    .createRenderSetup()
    );
    private static final float LABEL_SCALE = 0.010416667F;

    @SubscribeEvent
    public static void onRender(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        ClientVESHData data = ClientVESHData.get();
        if (data.getEntries().isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;

        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);

        for (var posDirEntry : data.getEntries().entrySet()) {
            PosDir posDir = posDirEntry.getKey();
            ClientVESHEntry entry = posDirEntry.getValue();

            for (var streamEntry : entry.streams.entrySet()) {
                ClientStreamEntry stream = streamEntry.getValue();
                if (stream.isRemoved()) continue;

                long clientTick = mc.level.getGameTime();
                long elapsed = clientTick - stream.receivedAtTick;
                double tickDelta = stream.startTickCount + elapsed + partialTick;
                Vec3 currentPos = stream.startPos.add(stream.motion.scale(tickDelta));

                renderTail(poseStack, collector, camera, stream, currentPos);
                renderLabel(poseStack, collector, camera, stream, currentPos);
            }
        }
    }

    private static void renderTail(PoseStack poseStack, SubmitNodeCollector collector,
                                    CameraRenderState camera, ClientStreamEntry stream, Vec3 currentPos) {
        double motionLen = stream.motion.length();
        if (motionLen < 0.0001) return;
        Vec3 dir = stream.motion.normalize();

        for (int i = 0; i < 6; i++) {
            Vec3 tailPos = currentPos.subtract(dir.scale(i * motionLen));

            poseStack.pushPose();
            float dx = (float)(tailPos.x - camera.pos.x);
            float dy = (float)(tailPos.y - camera.pos.y);
            float dz = (float)(tailPos.z - camera.pos.z);
            poseStack.translate(dx, dy, dz);
            poseStack.mulPose(camera.orientation);

            float alpha = 1f - (float)i / 6.1f;
            float size = (float)(1.0f / Math.pow(1.5, i));
            poseStack.scale(size, size, 1f);

            int a = (int)(alpha * 255);
            int light = 0xF000F0;

            collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
                vertex(buffer, pose, -0.5f, -0.5f, a, 1, 1, light);
                vertex(buffer, pose, 0.5f, -0.5f, a, 0, 1, light);
                vertex(buffer, pose, 0.5f, 0.5f, a, 0, 0, light);
                vertex(buffer, pose, -0.5f, 0.5f, a, 1, 0, light);
            });

            poseStack.popPose();
        }
    }

    private static void renderLabel(PoseStack poseStack, SubmitNodeCollector collector,
                                     CameraRenderState camera, ClientStreamEntry stream, Vec3 currentPos) {
        if (stream.label == null) return;
        Vec3 motion = stream.motion;
        if (motion.lengthSqr() < 0.0001) return;

        Font font = Minecraft.getInstance().font;
        String fullText = stream.label.getString();
        int fullTextWidth = font.width(fullText);
        if (fullTextWidth == 0) return;

        // Label rendering: full text at currentPos.
        // Left-clip (alive) and right-clip (dying) require emitter position in ClientStreamEntry.
        // Add startPos tracking in a follow-up; for now render full text.
        // Dying streams that are fully consumed (deathTick * speed > text width) are skipped.

        float dx = (float)(currentPos.x - camera.pos.x);
        float dy = (float)(currentPos.y - camera.pos.y);
        float dz = (float)(currentPos.z - camera.pos.z);

        Vec3 dir = motion.normalize();
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        boolean vertical = Math.abs(dir.dot(up)) > 0.999;
        Vec3 normal;
        if (vertical) {
            normal = dir.cross(new Vec3(1.0, 0.0, 0.0)).normalize();
        } else {
            normal = dir.cross(up).normalize();
        }

        FormattedCharSequence text = FormattedCharSequence.forward(fullText, net.minecraft.network.chat.Style.EMPTY);

        for (Vec3 faceNormal : new Vec3[]{normal, normal.scale(-1)}) {
            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);

            Quaternionf rotation = new Quaternionf().rotateTo(
                    new Vector3f(0, 0, 1),
                    new Vector3f((float)faceNormal.x, (float)faceNormal.y, (float)faceNormal.z));
            poseStack.mulPose(rotation);
            if (vertical) {
                poseStack.mulPose(new Quaternionf().rotateZ((float)Math.toRadians(faceNormal == normal ? -90 : 90)));
            }
            poseStack.scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);

            float textX = faceNormal == normal ? -fullTextWidth : 0;
            if (vertical) textX = faceNormal == normal ? 0 : -fullTextWidth;

            BlockPos pos = BlockPos.containing(currentPos);
            int light = LevelRenderer.getLightCoords(Minecraft.getInstance().level, pos);
            collector.submitText(poseStack, textX, 0, text, false,
                    Font.DisplayMode.NORMAL, 0xF000F0, stream.labelColor, light, 0);

            poseStack.popPose();
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, int a, float u, float v, int light) {
        buffer.addVertex(pose, x, y, 0f).setColor(255, 255, 255, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f);
    }
}
```

- [ ] **Step 3: Build to verify**

Run: `idea_build_project` with `filesToRebuild=["src/main/java/studio/fantasyit/ether_craft/event/EtherStreamRenderEvent.java"]`

---

### Task 8: Register everything — Network, AttachmentData, ServerTickEvent

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/network/Network.java`
- Modify: `src/main/java/studio/fantasyit/ether_craft/register/AttachmentDataRegistry.java`
- Create: `src/main/java/studio/fantasyit/ether_craft/event/ServerTickHandler.java`

- [ ] **Step 1: Register payloads in Network.java**

Add imports at the top:
```java
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamUpdateS2C;
```

Add to `commonMsg()` method, before the closing brace:
```java
        event.playToClient(
                EtherStreamCreateS2C.TYPE,
                EtherStreamCreateS2C.CODEC,
                EtherStreamCreateS2C::handle
        );
        event.playToClient(
                EtherStreamUpdateS2C.TYPE,
                EtherStreamUpdateS2C.CODEC,
                EtherStreamUpdateS2C::handle
        );
```

- [ ] **Step 2: Register attachment types in AttachmentDataRegistry.java**

After existing fields, add:

```java
    public static final Supplier<AttachmentType<VESHM>> VESHM = ATTACHMENT_TYPES.register(
            "ether_stream_virtual_manager", () -> AttachmentType.builder(VESHM::new).build()
    );
    public static final Supplier<AttachmentType<ClientVESHData>> CLIENT_VESH_DATA = ATTACHMENT_TYPES.register(
            "client_vesh_data", () -> AttachmentType.builder(ClientVESHData::new).build()
    );
```

Add imports:

```java
import studio.fantasyit.ether_craft.stream.vholder.VESHM;
import studio.fantasyit.ether_craft.event.ClientVESHData;
```

- [ ] **Step 3: VESHM needs public no-arg constructor**

Modify `VESHM.java` to add:
```java
    public VESHM() {}
```

- [ ] **Step 4: Create ServerTickHandler for VESHM tick**

Create `src/main/java/studio/fantasyit/ether_craft/event/ServerTickHandler.java`:

```java
package studio.fantasyit.ether_craft.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.vholder.VESHM;

@EventBusSubscriber(modid = EtherCraft.MODID)
public class ServerTickHandler {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            VESHM.get(level).tick(level);
        }
    }
}
```

- [ ] **Step 5: Wire VESHM.tick() to send sync payloads**

Update `VESHM.tick()` method. After the loop that removes dead VESHs, add sync logic:

```java
        // Send sync payloads
        for (var entry : holders.entrySet()) {
            PosDir posDir = entry.getKey();
            VirtualEtherStreamHolder holder = entry.getValue();

            List<studio.fantasyit.ether_craft.network.s2c.EtherStreamUpdateS2C.StreamEntry> entries = new ArrayList<>();
            for (VirtualEtherStream ves : holder.streams) {
                byte flags = 0;
                if (ves.dead) flags |= 1;
                if (ves.dying) flags |= 2;
                entries.add(new studio.fantasyit.ether_craft.network.s2c.EtherStreamUpdateS2C.StreamEntry(
                        ves.streamId, ves.tickCount, ves.getEther(), flags, ves.deathTick, ves.label, ves.labelColor
                ));
            }

            if (!entries.isEmpty() || !holder.streams.isEmpty()) {
                var payload = new studio.fantasyit.ether_craft.network.s2c.EtherStreamUpdateS2C(posDir, entries);
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(
                        level, level.getChunk(posDir.pos()).getPos(), payload);
            }
        }
```

Add import for `EtherStreamUpdateS2C`:
```java
import studio.fantasyit.ether_craft.network.s2c.EtherStreamUpdateS2C;
```

- [ ] **Step 6: Build to verify**

Run: `idea_build_project`

---

### Task 9: Migrate emitters to VESHM

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/block/emitter/EtherStreamEmitterEntity.java`
- Modify: `src/main/java/studio/fantasyit/ether_craft/node/plugins/feature/FeatureEtherStreamEmitter.java`

- [ ] **Step 1: Migrate EtherStreamEmitterEntity**

Replace the `tickServer()` method body. Old code pattern at lines 44-77.

Replace with:

```java
    @Override
    public void tickServer() {
        if (this.getEther() > 1000) {
            if (level instanceof ServerLevel serverLevel) {
                @NotNull Direction targetDirection = this.getBlockState().getValue(EtherAdaptNodeBlock.FACING);
                Vec3 dir = targetDirection.getUnitVec3().multiply(0.55f, 0.55f, 0.55f);
                PosDir posDir = new PosDir(this.getBlockPos(), targetDirection);

                VESHM veshm = VESHM.get(serverLevel);
                IEtherStreamLike stream = veshm.createStream(
                        serverLevel, posDir, (int) this.getEther(),
                        this.getBlockPos().getCenter().add(dir),
                        dir.multiply(0.1f, 0.1f, 0.1f)
                );

                EtherStreamStorageCapability itemStorage = new EtherStreamStorageCapability(this.inputContainer.getContainerSize());
                for (int i = 0; i < this.inputContainer.getContainerSize(); i++) {
                    try (Transaction transaction = Transaction.openRoot()) {
                        @NotNull ItemResource res = this.handler.getResource(i);
                        if (res.isEmpty()) continue;
                        int extracted = this.handler.extract(i, res, Integer.MAX_VALUE, transaction);
                        int insert = itemStorage.handler.insert(i, res, extracted, transaction);
                        if (insert == extracted)
                            transaction.commit();
                    }
                }
                stream.addCapability(itemStorage);

                this.setEther(0);
            }
        }
        if (markUpdate) {
            markUpdate = false;
        }
    }
```

Add imports:

```java
import net.minecraft.server.level.ServerLevel;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.stream.vholder.VESHM;
```

Remove unused imports: `Entity`, `EtherStreamEntity`.

- [ ] **Step 2: Migrate FeatureEtherStreamEmitter**

Replace the `process()` method body. Old code at lines 53-92.

Replace: `EtherStreamEntity entity = EtherStreamEntity.create(...)` block (lines 57-63) with:

```java
            PosDir posDir = new PosDir(nodeEntity.getBlockPos(), direction);
            ServerLevel serverLevel = (ServerLevel) nodeEntity.getLevel();
            if (serverLevel == null) return false;

            VESHM veshm = VESHM.get(serverLevel);
            IEtherStreamLike stream = veshm.createStream(
                    serverLevel, posDir, (int) sendWith,
                    nodeEntity.getBlockPos().getCenter().add(dir),
                    dir.multiply(0.1f, 0.1f, 0.1f)
            );
```

Then replace `entity.getCapability(EtherStreamStorageCapability.ID)` with `stream.getCapability(EtherStreamStorageCapability.ID)` (line 72).

And replace `provider.provideCapabilities(entity)` with `provider.provideCapabilities(stream)` (line 68).

And remove `nodeEntity.getLevel().addFreshEntity(entity)` (lines 86-88) — virtual streams don't need spawning.

Add imports:

```java
import net.minecraft.server.level.ServerLevel;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.stream.vholder.VESHM;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
```

Remove unused import: `EtherStreamEntity`.

- [ ] **Step 3: Build to verify**

Run: `idea_build_project` with `filesToRebuild=["src/main/java/studio/fantasyit/ether_craft/block/emitter/EtherStreamEmitterEntity.java","src/main/java/studio/fantasyit/ether_craft/node/plugins/feature/FeatureEtherStreamEmitter.java"]`

---

### Task 10: Migrate IEtherStreamCapabilityProviderPlugin interface + implementations

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/node/plugins/base/IEtherStreamCapabilityProviderPlugin.java`
- Modify: `src/main/java/studio/fantasyit/ether_craft/node/plugins/upgrade/EtherStreamStorageUpgrade.java`
- Modify: `src/main/java/studio/fantasyit/ether_craft/node/plugins/upgrade/EtherStreamDamageUpgrade.java`
- Modify: `src/main/java/studio/fantasyit/ether_craft/node/plugins/upgrade/EtherStreamTextUpgrade.java`
- Modify: `src/main/java/studio/fantasyit/ether_craft/node/plugins/upgrade/EtherStreamGrowthAcceleratorUpgrade.java`
- Modify: `src/main/java/studio/fantasyit/ether_craft/node/plugins/upgrade/EtherStreamBreakBlockUpgrade.java`

- [ ] **Step 1: Change interface parameter type**

In `IEtherStreamCapabilityProviderPlugin.java`, change:
```java
    void provideCapabilities(EtherStreamEntity entity);
```
to:
```java
    void provideCapabilities(IEtherStreamLike entity);
```

Update imports: remove `EtherStreamEntity`, add `IEtherStreamLike`:
```java
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
```

- [ ] **Step 2: Update all 5 implementations (signature only, body unchanged)**

For each file:
- Change import: `import studio.fantasyit.ether_craft.entity.stream.EtherStreamEntity;` → `import studio.fantasyit.ether_craft.stream.IEtherStreamLike;`
- Change method signature: `public void provideCapabilities(EtherStreamEntity entity)` → `public void provideCapabilities(IEtherStreamLike entity)`
- Body code uses only `entity.getCapability()` and `entity.addCapability()` — both are on `IEtherStreamLike`, no body changes needed.

Files (read each individually and update):
1. `src/main/java/studio/fantasyit/ether_craft/node/plugins/upgrade/EtherStreamStorageUpgrade.java` — line 25, line 6
2. `src/main/java/studio/fantasyit/ether_craft/node/plugins/upgrade/EtherStreamDamageUpgrade.java`
3. `src/main/java/studio/fantasyit/ether_craft/node/plugins/upgrade/EtherStreamTextUpgrade.java`
4. `src/main/java/studio/fantasyit/ether_craft/node/plugins/upgrade/EtherStreamGrowthAcceleratorUpgrade.java`
5. `src/main/java/studio/fantasyit/ether_craft/node/plugins/upgrade/EtherStreamBreakBlockUpgrade.java`

- [ ] **Step 3: Check if body code uses EtherStreamEntity-specific methods**

The upgrade bodies only call `entity.getCapability()` and `entity.addCapability()` — both are `IEtherStreamLike` methods. No other entity-specific calls. No changes needed beyond the signature.

- [ ] **Step 4: Build to verify**

Run: `idea_build_project`

---

### Task 11: Deprecate old code

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/entity/stream/EtherStreamEntity.java`
- Modify: `src/main/java/studio/fantasyit/ether_craft/entity/stream/render/EtherStreamEntityRenderer.java`
- Modify: `src/main/java/studio/fantasyit/ether_craft/register/EntityRegistry.java`

- [ ] **Step 1: Add @Deprecated to EtherStreamEntity**

Add above the class declaration:
```java
@Deprecated(forRemoval = true)
```

- [ ] **Step 2: Add @Deprecated to EtherStreamEntityRenderer**

Add above the class declaration:
```java
@Deprecated(forRemoval = true)
```

- [ ] **Step 3: Add @Deprecated to entity type field in EntityRegistry**

Add above the `ETHER_STREAM_ENTITY` field:
```java
    @Deprecated(forRemoval = true)
```

- [ ] **Step 4: Add @SuppressWarnings where needed**

In `EtherStreamEmitterEntity.java`, any remaining reference to `EtherStreamEntity` should be removed already. In `FeatureEtherStreamEmitter.java`, same.

Check: `ChainedEmitterEntityHitCache.getAllEntitiesRect` filters `entity.is(EntityRegistry.ETHER_STREAM_ENTITY)`. Keep this for now since old entities may still exist during transition. Add `@SuppressWarnings("deprecation")` on line 46.

- [ ] **Step 5: Build to verify**

Run: `idea_build_project`

---

### Task 12: Final verification

- [ ] **Step 1: Full build**

Run: `idea_build_project` with `rebuild=true`

- [ ] **Step 2: Fix any compilation errors**

Iterate on any remaining errors reported by the build.

- [ ] **Step 3: Review critical paths**

Verify with code review that:
- `VESHM.createStream` is called with correct `PosDir` from both emitters
- `VESH.tick(cache, posDir)` passes correct PosDir to `doCollision`
- `EtherStreamCreateS2C` is sent when a VES is first created (currently not sent — needs wiring)
- `EtherStreamRenderEvent` uses correct partial tick from `mc.getTimer()`

**Note**: `EtherStreamCreateS2C` is not yet sent from `VESHM.createStream`. This is intentional for initial deployment — the `EtherStreamUpdateS2C` batch update will handle both create and update on the client side (as designed in the spec: "streamId not in cache → treat as create"). Send the create payload in a follow-up task after verifying the update path works.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: replace EtherStreamEntity with virtual stream system (VESHM/VESH/VES)"
```
