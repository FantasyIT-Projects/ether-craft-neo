# Ether Stream Bounce-Back & Recreate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add bounce-back (reverse on destruction with residual ether) and generic recreate() to ether streams.

**Architecture:** Add `recreate(Vec3 newMotion)` to `IEtherStreamLike`, `onRecreate(IEtherStreamLike)` default to `IStreamCapability`. Both `VirtualEtherStream` and `EtherStreamEntity` implement recreate: create new stream, move capabilities (clear old), transfer data, call onRecreate, register. Destruction methods check `ether > 0` to trigger bounce-back. Holder loops changed to index-based to allow in-loop adds.

**Tech Stack:** Java 25, NeoForge 26.1.2, Minecraft

---

### Task 1: Add `recreate` to `IEtherStreamLike` and `onRecreate` to `IStreamCapability`

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/stream/IEtherStreamLike.java`
- Modify: `src/main/java/studio/fantasyit/ether_craft/stream/cap/IStreamCapability.java`

- [ ] **Step 1: Add `recreate` method to `IEtherStreamLike`**

In `src/main/java/studio/fantasyit/ether_craft/stream/IEtherStreamLike.java`, add after line 36 (`setRunIntoEtherGlass`):

```java
IEtherStreamLike recreate(Vec3 newMotion);
```

Also add the import for `Vec3` if not already present (it uses `Vec3` at line 8, so already imported).

Use `idea_replace_text_in_file`:
- `pathInProject`: `src/main/java/studio/fantasyit/ether_craft/stream/IEtherStreamLike.java`
- `oldText`: `    void setRunIntoEtherGlass(boolean isEtherGlass2);`
- `newText`: `    void setRunIntoEtherGlass(boolean isEtherGlass2);

    IEtherStreamLike recreate(Vec3 newMotion);`

- [ ] **Step 2: Add `onRecreate` default method to `IStreamCapability`**

In `src/main/java/studio/fantasyit/ether_craft/stream/cap/IStreamCapability.java`, add after line 34 (`shouldPassThrough(Entity entity)`):

```java
    default void onRecreate(IEtherStreamLike newStream) {
    }
```

Use `idea_replace_text_in_file`:
- `pathInProject`: `src/main/java/studio/fantasyit/ether_craft/stream/cap/IStreamCapability.java`
- `oldText`: `    default boolean shouldPassThrough(Entity entity) {
        return false;
    }`
- `newText`: `    default boolean shouldPassThrough(Entity entity) {
        return false;
    }

    default void onRecreate(IEtherStreamLike newStream) {
    }`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/stream/IEtherStreamLike.java src/main/java/studio/fantasyit/ether_craft/stream/cap/IStreamCapability.java
git commit -m "feat: add recreate() to IEtherStreamLike and onRecreate() to IStreamCapability"
```

---

### Task 2: Update `VirtualEtherStream` — holder field, recreate, bounce-back guard

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/stream/vholder/VirtualEtherStream.java`

- [ ] **Step 1: Add `holder` field and update constructor**

After line 44 (`List<IEtherStreamSyncedData> toSyncData = new ArrayList<>();`), add:

```java
    final VirtualEtherStreamHolder holder;
```

Modify the constructor (lines 45-55) to accept `VirtualEtherStreamHolder holder` and assign it. Replace the entire constructor:

Use `idea_replace_text_in_file`:
- `pathInProject`: `src/main/java/studio/fantasyit/ether_craft/stream/vholder/VirtualEtherStream.java`
- `oldText`: `    public VirtualEtherStream(int streamId, int ether, Vec3 startPos, PosDir posDir, Vec3 motion, Level level) {
        this.streamId = streamId;
        this.ether = ether;
        this.level = level;
        this.motion = motion;
        this.markToSyncCreation = true;
        this.startPos = this.pos = startPos;
        this.direction = posDir.dir();
        this.posDir = posDir;
        this.runIntoEtherGlass = level.getBlockState(BlockPos.containing(startPos)).is(BlockRegistry.ETHER_GLASS);
    }`
- `newText`: `    public VirtualEtherStream(int streamId, int ether, Vec3 startPos, PosDir posDir, Vec3 motion, Level level, VirtualEtherStreamHolder holder) {
        this.streamId = streamId;
        this.ether = ether;
        this.level = level;
        this.motion = motion;
        this.holder = holder;
        this.markToSyncCreation = true;
        this.startPos = this.pos = startPos;
        this.direction = posDir.dir();
        this.posDir = posDir;
        this.runIntoEtherGlass = level.getBlockState(BlockPos.containing(startPos)).is(BlockRegistry.ETHER_GLASS);
    }`

- [ ] **Step 2: Update `fromData` to accept holder**

Replace the `fromData` static method (lines 213-231):

Use `idea_replace_text_in_file`:
- `pathInProject`: `src/main/java/studio/fantasyit/ether_craft/stream/vholder/VirtualEtherStream.java`
- `oldText`: `    static VirtualEtherStream fromData(ServerLevel level, VirtualEtherStreamData data) {
        VirtualEtherStream ves = new VirtualEtherStream(
                data.streamId(),
                data.ether(),
                data.startPos(),
                data.posDir(),
                data.motion(),
                level
        );
        ves.pos = data.pos();
        ves.tickCount = data.tickCount();
        ves.consumer.fromState(data.consumerState());
        ves.capabilities.addAll(data.capabilities());
        for (IStreamCapability cap : data.capabilities()) {
            cap.setConsumer(ves.consumer);
        }
        ves.toSyncData = new ArrayList<>(data.toSyncData());
        return ves;
    }`
- `newText`: `    static VirtualEtherStream fromData(ServerLevel level, VirtualEtherStreamData data, VirtualEtherStreamHolder holder) {
        VirtualEtherStream ves = new VirtualEtherStream(
                data.streamId(),
                data.ether(),
                data.startPos(),
                data.posDir(),
                data.motion(),
                level,
                holder
        );
        ves.pos = data.pos();
        ves.tickCount = data.tickCount();
        ves.consumer.fromState(data.consumerState());
        ves.capabilities.addAll(data.capabilities());
        for (IStreamCapability cap : data.capabilities()) {
            cap.setConsumer(ves.consumer);
        }
        ves.toSyncData = new ArrayList<>(data.toSyncData());
        return ves;
    }`

- [ ] **Step 3: Implement `recreate` method**

Add after the `tick()` method (after line 167, before `clearSyncedData`). Insert the new method between the closing brace of `tick()` and the `clearSyncedData` method:

Use `idea_replace_text_in_file`:
- `pathInProject`: `src/main/java/studio/fantasyit/ether_craft/stream/vholder/VirtualEtherStream.java`
- `oldText`: `    @Override
    public void clearSyncedData(Identifier id) {`
- `newText`: `    @Override
    public IEtherStreamLike recreate(Vec3 newMotion) {
        PosDir newPosDir = new PosDir(BlockPos.containing(pos), Direction.getApproximateNearest(newMotion));
        VirtualEtherStream newStream = new VirtualEtherStream(
                holder.nextId++, ether, pos, newPosDir, newMotion, level, holder
        );
        newStream.capabilities = this.capabilities;
        this.capabilities = new ArrayList<>();
        for (IStreamCapability cap : newStream.capabilities) {
            cap.setConsumer(newStream.consumer);
        }
        newStream.consumer.fromState(this.consumer.toState());
        newStream.toSyncData = new ArrayList<>(this.toSyncData);
        newStream.tickCount = 0;
        for (IStreamCapability cap : newStream.capabilities) {
            cap.onRecreate(newStream);
        }
        newStream.markToSyncCreation = true;
        holder.streams.add(newStream);
        this.ether = 0;
        this.markDead();
        return newStream;
    }

    @Override
    public void clearSyncedData(Identifier id) {`

- [ ] **Step 4: Add bounce-back guard to `markDead`**

Replace `markDead()` method (lines 121-127):

Use `idea_replace_text_in_file`:
- `pathInProject`: `src/main/java/studio/fantasyit/ether_craft/stream/vholder/VirtualEtherStream.java`
- `oldText`: `    public void markDead() {
        if (markToRemove) return;
        for (IStreamCapability cap : capabilities) {
            cap.onDestroy(this);
        }
        markToRemove = true;
    }`
- `newText`: `    public void markDead() {
        if (markToRemove) return;
        if (ether > 0) {
            recreate(deltaMovement().reverse());
            return;
        }
        for (IStreamCapability cap : capabilities) {
            cap.onDestroy(this);
        }
        markToRemove = true;
    }`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/stream/vholder/VirtualEtherStream.java
git commit -m "feat: add holder field, recreate(), and bounce-back guard to VirtualEtherStream"
```

---

### Task 3: Update `VirtualEtherStreamHolder` — index-based loops, pass holder

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/stream/vholder/VirtualEtherStreamHolder.java`

- [ ] **Step 1: Update `createStream` to pass holder**

Replace line 50-57 in `createStream()`:

Use `idea_replace_text_in_file`:
- `pathInProject`: `src/main/java/studio/fantasyit/ether_craft/stream/vholder/VirtualEtherStreamHolder.java`
- `oldText`: `        VirtualEtherStream ves = new VirtualEtherStream(
                nextId++,
                ether,
                pos,
                posDir,
                motion,
                level
        );`
- `newText`: `        VirtualEtherStream ves = new VirtualEtherStream(
                nextId++,
                ether,
                pos,
                posDir,
                motion,
                level,
                this
        );`

- [ ] **Step 2: Update `loadFromData` to pass holder**

Replace line 357 in `loadFromData()`:

Use `idea_replace_text_in_file`:
- `pathInProject`: `src/main/java/studio/fantasyit/ether_craft/stream/vholder/VirtualEtherStreamHolder.java`
- `oldText`: `            VirtualEtherStream ves = VirtualEtherStream.fromData(level, data);`
- `newText`: `            VirtualEtherStream ves = VirtualEtherStream.fromData(level, data, this);`

- [ ] **Step 3: Convert tick foreach to index-based with size guard**

Replace line 74:
```
for (VirtualEtherStream ves : streams) ves.tick();
```

Use `idea_replace_text_in_file`:
- `pathInProject`: `src/main/java/studio/fantasyit/ether_craft/stream/vholder/VirtualEtherStreamHolder.java`
- `oldText`: `        for (VirtualEtherStream ves : streams) ves.tick();`
- `newText`: `        for (int i = 0, size = streams.size(); i < size; i++) streams.get(i).tick();`

- [ ] **Step 4: Convert tickCollideAll collision foreach to index-based**

Replace line 112:
```
        for (VirtualEtherStream ves : streams) {
```

Use `idea_replace_text_in_file`:
- `pathInProject`: `src/main/java/studio/fantasyit/ether_craft/stream/vholder/VirtualEtherStreamHolder.java`
- `oldText`: `        for (VirtualEtherStream ves : streams) {
            Vec3 oldPos = ves.pos;
            Vec3 newPos = oldPos.add(ves.motion);

            int clipStart = BlockPos.containing(oldPos).distManhattan(pos);`
- `newText`: `        for (int i = 0, size = streams.size(); i < size; i++) {
            VirtualEtherStream ves = streams.get(i);
            Vec3 oldPos = ves.pos;
            Vec3 newPos = oldPos.add(ves.motion);

            int clipStart = BlockPos.containing(oldPos).distManhattan(pos);`

- [ ] **Step 5: Convert ether glass foreach to index-based**

Replace line 190:
```
        for (VirtualEtherStream ves : streams) {
            BlockPos oldPos = BlockPos.containing(ves.pos);
```

Use `idea_replace_text_in_file`:
- `pathInProject`: `src/main/java/studio/fantasyit/ether_craft/stream/vholder/VirtualEtherStreamHolder.java`
- `oldText`: `        for (VirtualEtherStream ves : streams) {
            BlockPos oldPos = BlockPos.containing(ves.pos);`
- `newText`: `        for (int i = 0, size = streams.size(); i < size; i++) {
            VirtualEtherStream ves = streams.get(i);
            BlockPos oldPos = BlockPos.containing(ves.pos);`

- [ ] **Step 6: Convert `syncToPlayer` foreach to index-based (also modifies streams)**

Actually `syncToPlayer` doesn't modify streams in a way that would cause issues. But for consistency and safety:

Replace line 304:
```
        for (VirtualEtherStream ves : streams) {
            if (ves.markToRemove) continue;
```

Use `idea_replace_text_in_file`:
- `pathInProject`: `src/main/java/studio/fantasyit/ether_craft/stream/vholder/VirtualEtherStreamHolder.java`
- `oldText`: `        for (VirtualEtherStream ves : streams) {
            if (ves.markToRemove) continue;`
- `newText`: `        for (int i = 0, size = streams.size(); i < size; i++) {
            VirtualEtherStream ves = streams.get(i);
            if (ves.markToRemove) continue;`

- [ ] **Step 7: Convert `toData` foreach to index-based**

Replace line 345:
```
        for (VirtualEtherStream ves : streams) {
            if (!ves.markToRemove) {
```

Use `idea_replace_text_in_file`:
- `pathInProject`: `src/main/java/studio/fantasyit/ether_craft/stream/vholder/VirtualEtherStreamHolder.java`
- `oldText`: `        for (VirtualEtherStream ves : streams) {
            if (!ves.markToRemove) {`
- `newText`: `        for (int i = 0, size = streams.size(); i < size; i++) {
            VirtualEtherStream ves = streams.get(i);
            if (!ves.markToRemove) {`

- [ ] **Step 8: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/stream/vholder/VirtualEtherStreamHolder.java
git commit -m "feat: index-based loops, pass holder to VirtualEtherStream"
```

---

### Task 4: Update `EtherStreamEntity` — recreate, bounce-back guard

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/entity/stream/EtherStreamEntity.java`

- [ ] **Step 1: Implement `recreate` method**

Add the `recreate` method after `dropAndDiscard()` (after line 352, before `addAdditionalSaveData`):

Use `idea_replace_text_in_file`:
- `pathInProject`: `src/main/java/studio/fantasyit/ether_craft/entity/stream/EtherStreamEntity.java`
- `oldText`: `    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }`
- `newText`: `    @Override
    public IEtherStreamLike recreate(Vec3 newMotion) {
        EtherStreamEntity newEntity = EtherStreamEntity.create(level(), ether, position(), newMotion);
        newEntity.capabilities = this.capabilities;
        this.capabilities = new ArrayList<>();
        for (IStreamCapability cap : newEntity.capabilities) {
            cap.setConsumer(newEntity.consumer);
        }
        newEntity.consumer.fromState(this.consumer.toState());
        newEntity.toSyncData = new ArrayList<>(this.toSyncData);
        newEntity.ticked = false;
        for (IStreamCapability cap : newEntity.capabilities) {
            cap.onRecreate(newEntity);
        }
        level().addFreshEntity(newEntity);
        this.ether = 0;
        this.dropAndDiscard();
        return newEntity;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }`

- [ ] **Step 2: Add bounce-back guard to `dropAndDiscard`**

Replace `dropAndDiscard()` method (lines 340-352):

Use `idea_replace_text_in_file`:
- `pathInProject`: `src/main/java/studio/fantasyit/ether_craft/entity/stream/EtherStreamEntity.java`
- `oldText`: `    public void dropAndDiscard() {
        if (entityData.get(DYING)) return;
        for (IStreamCapability capability : capabilities) {
            capability.onDestroy(this);
        }
        if (entityData.get(LABEL_DATA).isPresent()) {
            deathTickStart = this.tickCount;
            entityData.set(DEATH_POS, new Vector3f((float) this.getX(), (float) this.getY(), (float) this.getZ()));
            entityData.set(DYING, true);
        } else {
            this.discard();
        }
    }`
- `newText`: `    public void dropAndDiscard() {
        if (entityData.get(DYING)) return;
        if (ether > 0) {
            recreate(deltaMovement().reverse());
            return;
        }
        for (IStreamCapability capability : capabilities) {
            capability.onDestroy(this);
        }
        if (entityData.get(LABEL_DATA).isPresent()) {
            deathTickStart = this.tickCount;
            entityData.set(DEATH_POS, new Vector3f((float) this.getX(), (float) this.getY(), (float) this.getZ()));
            entityData.set(DYING, true);
        } else {
            this.discard();
        }
    }`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/entity/stream/EtherStreamEntity.java
git commit -m "feat: add recreate() and bounce-back guard to EtherStreamEntity"
```

---

### Task 5: Build and verify

- [ ] **Step 1: Build the project**

Use `idea_build_project` with `rebuild=true`, `projectPath=D:/Minecraft/_proj/ether_craft`, `timeout=300000`.
Expected: BUILD SUCCESSFUL, no errors.

- [ ] **Step 2: Commit if all clean**

```bash
git log --oneline -5
```
