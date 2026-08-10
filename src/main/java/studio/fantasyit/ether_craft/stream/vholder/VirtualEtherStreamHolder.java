package studio.fantasyit.ether_craft.stream.vholder;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.network.s2c.*;
import studio.fantasyit.ether_craft.perf.ServerPerf;
import studio.fantasyit.ether_craft.plating.helper.PlatingChargingUtil;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.register.ItemRegistry;
import studio.fantasyit.ether_craft.register.Tags;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;
import studio.fantasyit.ether_craft.stream.data.CachedEtherStreamEntry;
import studio.fantasyit.ether_craft.stream.idx.AutoIndexPosDir;
import studio.fantasyit.ether_craft.stream.idx.IndexMappingManager;
import studio.fantasyit.ether_craft.util.EntityGetterUtil;
import studio.fantasyit.ether_craft.util.LevelUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class VirtualEtherStreamHolder {
    private final Direction direction;
    private final BlockPos pos;
    private final PosDir posDir;
    private final ServerLevel level;
    final List<VirtualEtherStream> streams = new ArrayList<>();
    private final List<VirtualEtherStream> pendingTrackingStreams = new ArrayList<>();
    private final List<VirtualEtherStream> pendingPropertyRegisterStreams = new ArrayList<>();
    private final List<VirtualEtherStream> pendingPropertyRemoveStreams = new ArrayList<>();
    private final Vec3i chunkVec;
    Int2IntOpenHashMap trackingPlayers = new Int2IntOpenHashMap();
    Int2IntOpenHashMap playerLastCreateId = new Int2IntOpenHashMap();
    int nextId = 0;
    private boolean lastHadStreamInUnloadedChunk = false;
    private int holderMaxDistance;
    CachedEtherStreamEntry lastCreateSnapshot = null;
    private int blockScanTickCounter = 0;
    private int cachedMaxClipDist = -1;
    //方块快照
    private BlockState[] cachedBlockStates;
    private BlockPos[] cachedBlockPoses;
    private VoxelShape[] cachedShapes;
    private boolean[] cachedSkip;
    private boolean[] cachedChanged;
    private boolean cachedHasAnyChanged = false;
    //maxDistance维护
    private boolean sameSpeedHolder = false;


    StreamHolderPropertyCounter propertyCounter = new StreamHolderPropertyCounter();

    private record BlockCollision(BlockHitResult hit, BlockState state) {
    }


    public VirtualEtherStreamHolder(PosDir posDir, VirtualEtherStreamHolderManager manager, @NotNull ServerLevel level) {
        this.level = level;
        this.pos = posDir.pos();
        this.direction = posDir.dir();
        this.posDir = posDir;
        chunkVec = posDir.dir().getUnitVec3i().multiply(16);
    }

    private IndexMappingManager indexMappingManager() {
        return level.getData(AttachmentDataRegistry.INDEX_MAPPING_MANAGER);
    }

    private AutoIndexPosDir posDirAutoIndexed() {
        return indexMappingManager().get(posDir);
    }

    public VirtualEtherStream createStream(int ether, float offset, float speed) {
        indexMappingManager().recordAndPrepareSend(posDir);
        VirtualEtherStream ves = new VirtualEtherStream(
                nextId++,
                ether,
                posDir,
                offset,
                speed,
                level,
                this
        );
        if (sameSpeedHolder && !streams.isEmpty()) {
            if (streams.getFirst().startSpeed != speed) {
                sameSpeedHolder = false;
            }
        } else if (streams.isEmpty()) {
            sameSpeedHolder = true;
        }
        streams.add(ves);
        markTrackingPending(ves);
        markPropertyRegisterPending(ves);

        return ves;
    }

    void markTrackingPending(VirtualEtherStream ves) {
        if (ves.trackingPending) return;
        ves.trackingPending = true;
        pendingTrackingStreams.add(ves);
    }

    void markPropertyRegisterPending(VirtualEtherStream ves) {
        if (ves.propertyRegisterPending) return;
        ves.propertyRegisterPending = true;
        pendingPropertyRegisterStreams.add(ves);
    }

    void markPropertyRemovePending(VirtualEtherStream ves) {
        if (!ves.propertyRegistered || ves.propertyRemovePending) return;
        ves.propertyRemovePending = true;
        pendingPropertyRemoveStreams.add(ves);
    }

    private void registerPendingProperties() {
        for (int i = 0, size = pendingPropertyRegisterStreams.size(); i < size; i++) {
            VirtualEtherStream ves = pendingPropertyRegisterStreams.get(i);
            ves.propertyRegisterPending = false;
            if (ves.markToRemove || ves.propertyRegistered) continue;
            propertyCounter.addStream(ves.getExtraProperty());
            ves.propertyRegistered = true;
        }
        pendingPropertyRegisterStreams.clear();
    }

    private void unregisterPendingProperties() {
        for (int i = 0, size = pendingPropertyRemoveStreams.size(); i < size; i++) {
            VirtualEtherStream ves = pendingPropertyRemoveStreams.get(i);
            ves.propertyRemovePending = false;
            if (!ves.propertyRegistered) continue;
            propertyCounter.removeStream(ves.getExtraProperty());
            ves.propertyRegistered = false;
        }
        pendingPropertyRemoveStreams.clear();
    }

    public boolean hasStreamInUnloadedChunk(int maxBlockDist) {
        int maxChunk = (maxBlockDist + 15) >> 4;
        BlockPos.MutableBlockPos mut = pos.mutable();
        for (int i = 0; i <= maxChunk; i++) {
            if (!LevelUtil.isLoadedIgnoreHeight(level, mut)) return true;
            mut.move(chunkVec);
        }
        return false;
    }

    public boolean isStreamBlocked() {
        return lastHadStreamInUnloadedChunk;
    }

    public void tick(PlayerPayloadAccumulator acc) {
        if (streams.isEmpty()) return;
        lastHadStreamInUnloadedChunk = hasStreamInUnloadedChunk(holderMaxDistance);
        if (lastHadStreamInUnloadedChunk) {
            blockScanTickCounter = Config.etherStreamBlockScanInterval;
            return;
        }

        ServerPerf.startRecording(posDir);
        updateMaxDistance();
        updateTracking();
        registerPendingProperties();

        if (!propertyCounter.isNoBlockCollide() && !propertyCounter.isDisplayTime()) {
            ensureBlockSnapshot(holderMaxDistance + 1);
        }

        //VES tick，包含以太量处理/位置变化等
        tickAllStreams();
        //碰撞
        tickCollideAll(holderMaxDistance);
        if (cachedHasAnyChanged) {
            Arrays.fill(cachedChanged, false);
            cachedHasAnyChanged = false;
        }
        //合并单格过密项
        mergeAll(holderMaxDistance);
        collectSync(acc);
        updateNoLongerTracking();
        unregisterPendingProperties();
        streams.removeIf(ves -> ves.markToRemove);
        if (streams.isEmpty()) sameSpeedHolder = false;
        ServerPerf.end(level);
    }

    private void tickAllStreams() {
        BlockState[] blockStates = cachedBlockStates;
        BlockPos[] blockPoses = cachedBlockPoses;
        VoxelShape[] shapes = cachedShapes;
        for (int i = 0, size = streams.size(); i < size; i++) {
            VirtualEtherStream ves = streams.get(i);
            if (ves.tickCount == 0) {
                int dist = ves.blockDistance();
                if (ves.getExtraProperty().noBlockHit && blockStates != null && blockPoses != null && shapes != null)
                    ves.firstBlock(blockPoses[dist], blockStates[dist], shapes[dist]);
                ves.firstTick();
            }
            if (ves.isDisplayTime()) ves.displayTimeTick();
            else ves.tick();
        }
    }

    private void updateMaxDistance() {
        if (sameSpeedHolder && !streams.isEmpty()) {
            VirtualEtherStream fs = streams.getFirst();
            holderMaxDistance = fs.blockDistance() + 1;
            return;
        }
        int nxtMaxDist = 0;
        boolean sameSpeed = true;
        float speed = 0;
        for (VirtualEtherStream ves : streams) {
            if (!ves.markToRemove) {
                int distance = ves.blockDistance();
                if (distance > nxtMaxDist) {
                    nxtMaxDist = distance;
                }
                if (sameSpeed) {
                    if (speed == 0)
                        speed = ves.startSpeed;
                    if (speed != ves.startSpeed)
                        sameSpeed = false;
                }
            }
        }
        if (sameSpeed)
            sameSpeedHolder = true;
        holderMaxDistance = nxtMaxDist + 1;
    }

    private void mergeAll(int maxDistance) {
        if (streams.size() < Config.etherStreamDestroyThreshold) return;
        int size = maxDistance + 2;
        int[] streamCountAt = new int[size];
        for (int i = streams.size() - 1; i >= 0; i--) {
            VirtualEtherStream ves = streams.get(i);
            int d = pos.distManhattan(ves.blockPosition());
            if (d < 0 || d >= size) continue;
            streamCountAt[d]++;
            if (streamCountAt[d] > Config.etherStreamDestroyThreshold) {
                ves.ether = 0;
                ves.markDead(null);
            }
        }
    }

    private void ensureBlockSnapshot(int maxClipDist) {
        boolean expired = ++blockScanTickCounter >= Config.etherStreamBlockScanInterval;
        boolean tooSmall = cachedBlockStates == null || cachedMaxClipDist < maxClipDist;
        if (!expired && !tooSmall) return;
        blockScanTickCounter = 0;
        cachedMaxClipDist = maxClipDist;
        BlockState[] blockStates = new BlockState[maxClipDist + 1];
        BlockPos[] blockPoses = new BlockPos[maxClipDist + 1];
        VoxelShape[] shapes = new VoxelShape[maxClipDist + 1];
        boolean[] skip = new boolean[maxClipDist + 1];
        boolean[] changed = new boolean[maxClipDist + 1];
        BlockState[] prevStates = cachedBlockStates;
        int prevLen = prevStates == null ? 0 : prevStates.length;
        boolean hasAnyChanged = false;
        BlockPos.MutableBlockPos blockScanPos = pos.mutable();
        for (int i = 0; i <= maxClipDist; i++) {
            BlockState blockState = level.getBlockState(blockScanPos);
            blockStates[i] = blockState;
            blockPoses[i] = blockScanPos.immutable();
            shapes[i] = blockState.getCollisionShape(level, blockScanPos);
            skip[i] = blockState.isAir() || blockState.is(Tags.ETHER_STREAM_PASS_THROUGH) || shapes[i].isEmpty();
            changed[i] = i < prevLen && blockState != prevStates[i];
            hasAnyChanged |= changed[i];
            blockScanPos.move(direction);
        }
        cachedBlockStates = blockStates;
        cachedBlockPoses = blockPoses;
        cachedShapes = shapes;
        cachedSkip = skip;
        cachedChanged = changed;
        cachedHasAnyChanged = hasAnyChanged;
    }

    @Nullable
    BlockState getBlockState(int dist) {
        if (cachedBlockStates == null) return null;
        return cachedBlockStates[dist];
    }

    boolean isBlockChanged(int dist) {
        return cachedChanged != null && cachedChanged[dist];
    }

    @Nullable
    VoxelShape getCollisionShape(int dist) {
        if (cachedShapes == null) return null;
        return cachedShapes[dist];
    }

    boolean isFullBlock(int dist) {
        VoxelShape shape = getCollisionShape(dist);
        return shape != null && Block.isShapeFullBlock(shape);
    }

    private void tickCollideAll(int maxBlockDist) {
        boolean needBlockCollide = !propertyCounter.isNoBlockCollide();
        boolean needEntityCollide = !propertyCounter.isNoEntityCollide();
        if (!needEntityCollide && !needBlockCollide) return;
        BlockState[] blockStates = cachedBlockStates;
        BlockPos[] blockPoses = cachedBlockPoses;
        VoxelShape[] shapes = cachedShapes;
        boolean[] skip = cachedSkip;
        List<Entity> canHitEntity = null;
        if (needEntityCollide) {
            Vec3 queryVec = direction.getUnitVec3().scale(maxBlockDist + 1);
            canHitEntity = EntityGetterUtil.getEntities(level, new AABB(pos).expandTowards(queryVec).inflate(1.0));
            canHitEntity.removeIf(this::entityNoCollidePredicator);
        }

        for (int i = 0, size = streams.size(); i < size; i++) {
            VirtualEtherStream ves = streams.get(i);
            if (ves.markToRemove) continue;
            double d = ves.currentDistance;
            double nx = ves.offsetCenter.x + ves.offsetUnit.x * d;
            double ny = ves.offsetCenter.y + ves.offsetUnit.y * d;
            double nz = ves.offsetCenter.z + ves.offsetUnit.z * d;
            double ox = nx - ves.motion.x;
            double oy = ny - ves.motion.y;
            double oz = nz - ves.motion.z;

            //DisplayTime流：仅极简实体判定(contains)，命中即消失
            if (ves.isDisplayTime()) {
                if (needEntityCollide && !ves.getExtraProperty().noEntityHit && hitEntityForDisplayTime(ves, nx, ny, nz, canHitEntity)) {
                    ves.markDead(null);
                }
                continue;
            }

            BlockCollision blockCollision = null;
            double blockDist = Double.MAX_VALUE;
            if (needBlockCollide && blockStates != null && !ves.getExtraProperty().noBlockHit) {
                int clipStart = Math.clamp(ves.blockDistancePrev(), 0, blockStates.length - 1);
                int clipEnd = Math.clamp(ves.blockDistance(), 0, blockStates.length - 1);
                //计算是否可以跳过
                boolean noSkip = false;
                for (int j = clipStart; j <= clipEnd; j++) {
                    if (!skip[j]) {
                        noSkip = true;
                        break;
                    }
                }
                if (noSkip) {
                    //获取最近的方块碰撞
                    blockCollision = collideTryBlock(ves, blockStates, blockPoses, skip, shapes,
                            clipStart, clipEnd, nx, ny, nz, ox, oy, oz);
                    if (blockCollision != null) {
                        Vec3 hitLoc = blockCollision.hit().getLocation();
                        blockDist = (ox - hitLoc.x) * (ox - hitLoc.x)
                                + (oy - hitLoc.y) * (oy - hitLoc.y)
                                + (oz - hitLoc.z) * (oz - hitLoc.z);
                    }
                }
            }

            //判断比方块更近的实体碰撞
            EntityHitResult entityHit = null;
            if (needEntityCollide && !ves.getExtraProperty().noEntityHit) {
                entityHit = collideTryEntity(ves, canHitEntity, blockDist, nx, ny, nz, ox, oy, oz);
            }

            //确认将碰到entity
            if (entityHit != null) {
                commonHitEntity(entityHit, ves);
            } else if (blockCollision != null) {
                commonHitBlock(blockCollision, ves);
            }
        }

        if (needBlockCollide && blockStates != null) {
            for (int i = 0, size = streams.size(); i < size; i++) {
                VirtualEtherStream ves = streams.get(i);
                if (ves.markToSyncCreation || ves.markToRemove || ves.isDisplayTime()) continue;
                int bdp = ves.blockDistancePrev();
                int bd = ves.blockDistance();
                int id2 = Math.clamp(bd, 0, blockStates.length - 1);
                boolean crossed = bdp != bd;
                boolean changedAtCurrent = isBlockChanged(id2);
                if (!crossed && !changedAtCurrent) continue;
                double d = ves.currentDistance;
                BlockPos newPos = BlockPos.containing(
                        ves.offsetCenter.x + ves.offsetUnit.x * d,
                        ves.offsetCenter.y + ves.offsetUnit.y * d,
                        ves.offsetCenter.z + ves.offsetUnit.z * d);
                ves.onRunIntoNewBlock(newPos, blockStates[id2], shapes[id2]);
            }
        }
    }

    private boolean hitEntityForDisplayTime(VirtualEtherStream ves, double nx, double ny, double nz, List<Entity> entities) {
        for (Entity entity : entities) {
            if (entity.getBoundingBox().inflate(0.3).contains(nx, ny, nz)) return true;
        }
        return false;
    }

    private boolean entityNoCollidePredicator(Entity entity) {
        if (entity instanceof ItemEntity ie) {
            if (PlatingUtil.isPlatedItemEntity(ie)) return false;
            return !ie.getItem().is(Items.GLASS);
        }
        return false;
    }

    private void commonHitBlock(BlockCollision blockCollision, VirtualEtherStream ves) {
        BlockHitResult blockHit = blockCollision.hit();
        BlockState hitBlockState = blockCollision.state();
        boolean handled = false;
        if (hitBlockState.getBlock() instanceof ShelfBlock) {
            if (level.getBlockEntity(blockHit.getBlockPos()) instanceof ShelfBlockEntity shelf) {
                PlatingChargingUtil.tryChargeShelf(ves, shelf);
            }
        }
        for (IStreamCapability cap : ves.capabilities) {
            handled |= cap.hitBlock(level, ves, blockHit, hitBlockState);
        }
        if (handled) {
            markBlockSkipped(blockHit.getBlockPos());
        } else {
            ves.markDead(blockHit);
        }
    }

    public void markBlockSkipped(BlockPos blockPos) {
        if (cachedBlockStates == null || cachedBlockPoses == null) return;
        int index = blockPos.get(direction.getAxis()) - pos.get(direction.getAxis());
        if (direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            index = -index;
        }
        if (index < 0 || index >= cachedBlockStates.length) return;
        cachedSkip[index] = true;
        cachedBlockStates[index] = Blocks.AIR.defaultBlockState();
        cachedShapes[index] = Shapes.empty();
    }

    private void commonHitEntity(EntityHitResult entityHit, VirtualEtherStream ves) {
        boolean handled = false;
        if (entityHit.getEntity() instanceof ItemEntity ie && PlatingUtil.isPlatedItemEntity(ie)) {
            addEtherToPlatedItem(ves, ie);
        } else if (entityHit.getEntity() instanceof ItemEntity ie && ie.getItem().is(Items.GLASS)) {
            ie.setItem(new ItemStack(ItemRegistry.ETHER_GLASS_ITEM, ie.getItem().getCount()));
        } else {
            for (IStreamCapability cap : ves.capabilities) {
                handled |= cap.hitEntity(level, ves, entityHit, entityHit.getEntity());
            }
        }
        if (!handled) {
            PlatingChargingUtil.tryChargeEntity(ves, entityHit.getEntity());
            ves.markDead(entityHit);
        }
    }

    private @Nullable EntityHitResult collideTryEntity(VirtualEtherStream ves, List<Entity> entities, double blockDist,
                                                       double nx, double ny, double nz, double ox, double oy, double oz) {
        Entity hitEntity = null;
        Vec3 oldPos = new Vec3(ox, oy, oz);
        Vec3 newPos = new Vec3(nx, ny, nz);
        double nearestDist = blockDist;
        EntityHitResult hit = null;
        Vec3 entityHitAt = null;
        for (Entity entity : entities) {
            if (entity.is(Tags.ETHER_STREAM_PASS_THROUGH_ENTITY))
                continue;
            AABB bb = entity.getBoundingBox().inflate(0.3);
            double localDist = entity.distanceToSqr(oldPos);
            boolean currentCanHit = bb.contains(oldPos) && localDist < nearestDist;
            Vec3 localHitAt = bb.getCenter();
            if (!currentCanHit) {
                Optional<Vec3> clip = bb.clip(oldPos, newPos);
                if (clip.isPresent()) {
                    localDist = clip.get().distanceToSqr(oldPos);
                    if (localDist < nearestDist) {
                        currentCanHit = true;
                        localHitAt = clip.get();
                    }
                }
            }
            if (currentCanHit) {
                nearestDist = localDist;
                hitEntity = entity;
                entityHitAt = localHitAt;
            }
        }
        if (hitEntity != null) {
            hit = new EntityHitResult(hitEntity, entityHitAt);
        }
        return hit;
    }

    private @Nullable BlockCollision collideTryBlock(VirtualEtherStream ves, BlockState[] blockStates, BlockPos[] blockPoses,
                                                     boolean[] skip, VoxelShape[] shapes,
                                                     int clipStart, int clipEnd, double nx, double ny, double nz, double ox, double oy, double oz) {
        for (int j = clipStart; j <= clipEnd; j++) {
            if (skip[j]) continue;
            BlockState blockState = blockStates[j];
            BlockPos pos = blockPoses[j];
            boolean passThrough = false;
            for (IStreamCapability cap : ves.capabilities) {
                if (cap.shouldPassThrough(blockState, level, pos)) {
                    passThrough = true;
                    break;
                }
            }
            if (passThrough)
                continue;
            BlockHitResult hit = shapes[j].clip(new Vec3(ox, oy, oz), new Vec3(nx, ny, nz), pos);
            if (hit != null) {
                return new BlockCollision(hit, blockState);
            }
        }
        return null;
    }

    private void updateTracking() {
        if (pendingTrackingStreams.isEmpty()) return;
        for (int i = 0, size = pendingTrackingStreams.size(); i < size; i++) {
            VirtualEtherStream ves = pendingTrackingStreams.get(i);
            ves.trackingPending = false;
            if (ves.markToRemove) continue;
            if (ves.trackingDirty) {
                for (int j : ves.lastTrackingPlayers)
                    trackingPlayers.addTo(j, -1);
            }
            if (ves.trackingInitial || ves.trackingDirty) {
                for (int j : ves.trackingPlayers)
                    trackingPlayers.addTo(j, 1);
                if (ves.trackingDirty) {
                    ves.lastTrackingPlayers = new IntOpenHashSet(ves.trackingPlayers);
                    ves.trackingDirty = false;
                }
                ves.trackingInitial = false;
            }
        }
        pendingTrackingStreams.clear();
    }

    private void updateNoLongerTracking() {
        for (VirtualEtherStream ves : streams) {
            if (ves.markToRemove) {
                for (int i : ves.trackingPlayers)
                    trackingPlayers.addTo(i, -1);
            }
        }
        trackingPlayers.int2IntEntrySet().removeIf(e -> {
            if (e.getIntValue() <= 0) {
                playerLastCreateId.remove(e.getIntKey());
                return true;
            }
            return false;
        });
    }

    void clearPlayerQuickState(int playerId) {
        playerLastCreateId.remove(playerId);
    }

    private void collectSync(PlayerPayloadAccumulator acc) {
        List<VirtualEtherStream> collectedToCreate = new ArrayList<>();
        List<Integer> collectedToRemove = new ArrayList<>();
        List<VirtualEtherStream> collectedToSyncData = new ArrayList<>();
        List<VirtualEtherStream> collectedToSyncEtherConsume = new ArrayList<>();
        for (VirtualEtherStream ves : streams) {
            if (ves.markToSyncCreation && ves.markToRemove) continue;
            if (ves.markToRemove)
                collectedToRemove.add(ves.streamId);
            else if (ves.markToSyncCreation) {
                collectedToCreate.add(ves);
                ves.markToSyncCreation = false;
                ves.markToSyncData = false;
            } else {
                if (ves.markToSyncData) {
                    collectedToSyncData.add(ves);
                    ves.markToSyncData = false;
                }
                if (ves.needsEtherSync && ves.ether > 0) {
                    collectedToSyncEtherConsume.add(ves);
                    ves.needsEtherSync = false;
                }
            }
        }
        IntSet tracking = trackingPlayers.keySet();
        AutoIndexPosDir posDirAI = posDirAutoIndexed();

        if (!collectedToCreate.isEmpty()) {
            for (VirtualEtherStream ves : collectedToCreate) {
                if (ves.consumer.isDirty()) {
                    ves.consumer.recompute(ves, ves.capabilities);
                }

                boolean quickEligible = Config.etherStreamSyncDistance > 0
                        && lastCreateSnapshot != null
                        && snapshotMatches(lastCreateSnapshot, ves);

                if (quickEligible) {
                    IntArraySet quickPlayers = new IntArraySet();
                    IntArraySet fullPlayers = new IntArraySet();
                    for (int pid : ves.trackingPlayers) {
                        if (playerLastCreateId.containsKey(pid)
                                && playerLastCreateId.get(pid) == lastCreateSnapshot.streamId()) {
                            quickPlayers.add(pid);
                        } else {
                            fullPlayers.add(pid);
                        }
                    }
                    if (!fullPlayers.isEmpty()) {
                        EtherStreamInitialCreateS2C etherStreamCreateS2C = new EtherStreamInitialCreateS2C(
                                posDirAI, ves.streamId, ves.startOffset, ves.startSpeed,
                                ves.ether, ves.consumer.toState(), ves.toSyncData
                        );
                        acc.add(fullPlayers, etherStreamCreateS2C);
                    }
                    if (!quickPlayers.isEmpty()) {
                        acc.add(quickPlayers, new EtherStreamQuickCreateS2C(posDirAI));
                    }
                } else {
                    EtherStreamInitialCreateS2C etherStreamCreateS2C = new EtherStreamInitialCreateS2C(
                            posDirAI, ves.streamId, ves.startOffset, ves.startSpeed,
                            ves.ether, ves.consumer.toState(), ves.toSyncData
                    );
                    acc.add(ves.trackingPlayers, etherStreamCreateS2C);
                }

                for (int pid : ves.trackingPlayers) {
                    playerLastCreateId.put(pid, ves.streamId);
                }
                lastCreateSnapshot = createSnapshot(ves);
            }
        }

        if (!collectedToRemove.isEmpty()) {
            EtherStreamSetDyingS2C payload = new EtherStreamSetDyingS2C(posDirAI, collectedToRemove);
            acc.add(tracking, payload);
        }


        if (!collectedToSyncData.isEmpty()) {
            for (VirtualEtherStream ves : collectedToSyncData) {
                EtherStreamSyncDataS2C payload = new EtherStreamSyncDataS2C(posDirAI, ves.streamId, ves.toSyncData);
                acc.add(tracking, payload);
            }
        }

        if (!collectedToSyncEtherConsume.isEmpty()) {
            List<EtherStreamUpdateS2C.StreamEntry> updateEntries = new ArrayList<>();
            for (VirtualEtherStream ves : collectedToSyncEtherConsume) {
                if (ves.consumer.isDirty()) {
                    ves.consumer.recompute(ves, ves.capabilities);
                    ves.needsEtherConsumerSync = true;
                }
                EtherStreamUpdateS2C.StreamEntry streamEntry = new EtherStreamUpdateS2C.StreamEntry(
                        ves.streamId,
                        ves.ether,
                        ves.needsEtherConsumerSync ? Optional.of(ves.consumer.toState()) : Optional.empty()
                );
                ves.needsEtherConsumerSync = false;
                updateEntries.add(streamEntry);
            }
            EtherStreamUpdateS2C payload = new EtherStreamUpdateS2C(posDirAI, updateEntries);
            acc.add(tracking, payload);
        }
    }

    public VirtualEtherStream findStreamById(int id) {
        for (VirtualEtherStream ves : streams) {
            if (ves.streamId == id) return ves;
        }
        return null;
    }

    void syncAndStartTrackingByPlayer(ServerPlayer player) {
        List<EtherStreamBatchCreateS2C.StreamEntry> entries = new ArrayList<>();
        for (int i = 0, size = streams.size(); i < size; i++) {
            VirtualEtherStream ves = streams.get(i);
            if (ves.markToRemove) continue;
            if (ves.consumer.isDirty()) {
                ves.consumer.recompute(ves, ves.capabilities);
            }
            if (Config.etherStreamSyncDistance <= 0 || ves.position().distanceTo(player.position()) <= Config.etherStreamSyncDistance) {
                ves.addTrackingPlayer(player.getId());
                entries.add(new EtherStreamBatchCreateS2C.StreamEntry(
                        ves.streamId,
                        ves.startOffset,
                        ves.startSpeed,
                        ves.ether,
                        ves.tickCount,
                        ves.consumer.toState(),
                        new ArrayList<>(ves.toSyncData)
                ));
            }
        }
        if (!entries.isEmpty()) {
            EtherStreamBatchCreateS2C payload = new EtherStreamBatchCreateS2C(posDirAutoIndexed(), entries);
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public boolean isDead() {
        return streams.isEmpty();
    }


    private static CachedEtherStreamEntry createSnapshot(VirtualEtherStream ves) {
        if (!ves.toSyncData.isEmpty()) return null;
        return new CachedEtherStreamEntry(
                ves.getStreamId(),
                ves.startOffset,
                ves.startSpeed,
                ves.getEther(),
                1,
                ves.consumer.toState()
        );
    }

    private static boolean snapshotMatches(CachedEtherStreamEntry snapshot, VirtualEtherStream ves) {
        return snapshot.streamId() == ves.getStreamId() - 1
                && Float.compare(snapshot.startOffset(), ves.startOffset) == 0
                && Float.compare(snapshot.startSpeed(), ves.startSpeed) == 0
                && snapshot.ether() == ves.getEther()
                && snapshot.consumerState().equals(ves.consumer.toState())
                && ves.toSyncData.isEmpty();
    }

    private static void addEtherToPlatedItem(VirtualEtherStream ves, ItemEntity ie) {
        ItemStack stack = ie.getItem();
        int ether = ves.getEther();
        if (ether <= 0) return;
        PlatingUtil.addEther(stack, Math.min(ether, Config.platingMaxEtherReceive));
        ves.consumeEther(ether);
        ie.setItem(stack);
    }

    VirtualEtherStreamHolderData toData() {
        List<VirtualEtherStreamData> streamDataList = new ArrayList<>();
        for (int i = 0, size = streams.size(); i < size; i++) {
            VirtualEtherStream ves = streams.get(i);
            if (!ves.markToRemove) {
                streamDataList.add(ves.toData());
            }
        }
        return new VirtualEtherStreamHolderData(nextId, streamDataList);
    }


    public void loadFromData(VirtualEtherStreamHolderData holderData) {
        nextId = holderData.nextId();
        for (VirtualEtherStreamData data : holderData.streams()) {
            VirtualEtherStream ves = VirtualEtherStream.fromData(level, data, this);
            streams.add(ves);
            markTrackingPending(ves);
            markPropertyRegisterPending(ves);
        }
        int nxtMaxDist = 0;
        for (VirtualEtherStream ves : streams) {
            if (!ves.markToRemove) {
                int distance = ves.blockPosition().distManhattan(posDir.pos()) + Mth.ceil(ves.startSpeed);
                if (distance > nxtMaxDist) {
                    nxtMaxDist = distance;
                }
            }
        }
        this.holderMaxDistance = nxtMaxDist + 1;
    }
}
