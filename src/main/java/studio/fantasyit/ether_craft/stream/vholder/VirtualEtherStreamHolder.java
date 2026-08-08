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
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
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
import java.util.List;
import java.util.Optional;

public class VirtualEtherStreamHolder {
    private final Direction direction;
    private final BlockPos pos;
    private final PosDir posDir;
    private final VirtualEtherStreamHolderManager manager;
    private final ServerLevel level;
    final List<VirtualEtherStream> streams = new ArrayList<>();
    private final List<VirtualEtherStream> pendingTrackingStreams = new ArrayList<>();
    private final Vec3i chunkVec;
    Int2IntOpenHashMap trackingPlayers = new Int2IntOpenHashMap();
    Int2IntOpenHashMap playerLastCreateId = new Int2IntOpenHashMap();
    int nextId = 0;
    private boolean lastHadStreamInUnloadedChunk = false;
    private int holderMaxDistance;
    CachedEtherStreamEntry lastCreateSnapshot = null;
    private int blockScanTickCounter = 0;
    private int cachedMaxClipDist = -1;
    private BlockState[] cachedBlockStates;
    private BlockPos[] cachedBlockPoses;
    private VoxelShape[] cachedShapes;
    private boolean[] cachedSkip;

    StreamHolderPropertyCounter propertyCounter = new StreamHolderPropertyCounter();


    public VirtualEtherStreamHolder(PosDir posDir, VirtualEtherStreamHolderManager manager, @NotNull ServerLevel level) {
        this.manager = manager;
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
        streams.add(ves);
        markTrackingPending(ves);
        return ves;
    }

    void markTrackingPending(VirtualEtherStream ves) {
        if (ves.trackingPending) return;
        ves.trackingPending = true;
        pendingTrackingStreams.add(ves);
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
        if (lastHadStreamInUnloadedChunk) return;

        ServerPerf.startRecording(posDir);
        updateTracking();

        for (int i = 0, size = streams.size(); i < size; i++) {
            VirtualEtherStream ves = streams.get(i);
            if (ves.markToRemove || ves.propertyRegistered) continue;
            propertyCounter.addStream(ves.getExtraProperty());
            ves.propertyRegistered = true;
        }

        //VES tick，包含以太量处理/位置变化等
        for (int i = 0, size = streams.size(); i < size; i++) {
            VirtualEtherStream ves = streams.get(i);
            if (ves.isDisplayTime()) ves.displayTimeTick();
            else ves.tick();
        }

        //碰撞
        tickCollideAll(holderMaxDistance);
        //合并单格过密项
        mergeAll(holderMaxDistance);

        holderMaxDistance = getNxtMaxDist() + 1;
        collectSync(acc);
        updateNoLongerTracking();
        for (int i = 0, size = streams.size(); i < size; i++) {
            VirtualEtherStream ves = streams.get(i);
            if (!ves.markToRemove || !ves.propertyRegistered) continue;
            propertyCounter.removeStream(ves.getExtraProperty());
            ves.propertyRegistered = false;
        }
        streams.removeIf(ves -> ves.markToRemove);
        ServerPerf.end(level);
    }

    private int getNxtMaxDist() {
        int nxtMaxDist = 0;
        for (VirtualEtherStream ves : streams) {
            if (!ves.markToRemove) {
                int distance = ves.blockDistance();
                if (distance > nxtMaxDist) {
                    nxtMaxDist = distance;
                }
            }
        }
        return nxtMaxDist;
    }

    private void mergeAll(int maxDistance) {
        if (streams.isEmpty()) return;
        int size = maxDistance + 2;
        int[] streamCountAt = new int[size];
        for (int i = streams.size() - 1; i >= 0; i--) {
            VirtualEtherStream ves = streams.get(i);
            int d = pos.distManhattan(ves.blockPosition());
            if (d < 0) continue;
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
        BlockPos.MutableBlockPos blockScanPos = pos.mutable();
        for (int i = 0; i <= maxClipDist; i++) {
            BlockState blockState = level.getBlockState(blockScanPos);
            blockStates[i] = blockState;
            blockPoses[i] = blockScanPos.immutable();
            shapes[i] = blockState.getCollisionShape(level, blockScanPos);
            skip[i] = blockState.isAir() || blockState.is(Tags.ETHER_STREAM_PASS_THROUGH) || shapes[i].isEmpty();
            blockScanPos.move(direction);
        }
        cachedBlockStates = blockStates;
        cachedBlockPoses = blockPoses;
        cachedShapes = shapes;
        cachedSkip = skip;
    }

    private void tickCollideAll(int maxBlockDist) {
        int maxClipDist = maxBlockDist + 1;
        boolean needBlockCollide = !propertyCounter.isNoBlockCollide();
        boolean needEntityCollide = !propertyCounter.isNoEntityCollide();
        if (!needEntityCollide && !needBlockCollide) return;
        boolean isDisplayTimeFull = propertyCounter.isDisplayTime();

        if (needBlockCollide && !isDisplayTimeFull) {
            ensureBlockSnapshot(maxClipDist);
        }
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
            Vec3 newPos = ves.position();
            Vec3 oldPos = newPos.subtract(ves.motion);

            //DisplayTime流：仅极简实体判定(contains)，命中即消失
            if (ves.isDisplayTime()) {
                if (needEntityCollide && !ves.getExtraProperty().noEntityHit && hitEntityForDisplayTime(ves, newPos, canHitEntity)) {
                    ves.markDead(null);
                }
                continue;
            }

            BlockHitResult blockHit = null;
            double blockDist = Double.MAX_VALUE;
            if (needBlockCollide && !ves.getExtraProperty().noBlockHit) {
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
                    blockHit = collideTryBlock(ves, blockStates, blockPoses, skip, shapes, clipStart, clipEnd, oldPos, newPos);
                    if (blockHit != null) {
                        blockDist = oldPos.distanceToSqr(blockHit.getLocation());
                    }
                }
            }

            //判断比方块更近的实体碰撞
            EntityHitResult entityHit = null;
            if (needEntityCollide && !ves.getExtraProperty().noEntityHit) {
                entityHit = collideTryEntity(ves, canHitEntity, blockDist, oldPos, newPos);
            }

            //确认将碰到entity
            if (entityHit != null) {
                commonHitEntity(entityHit, ves);
            } else if (blockHit != null) {
                commonHitBlock(blockHit, ves);
            }
        }

        if (needBlockCollide) {
            for (int i = 0, size = streams.size(); i < size; i++) {
                VirtualEtherStream ves = streams.get(i);
                if (ves.markToSyncCreation || ves.markToRemove || ves.isDisplayTime()) continue;
                int bdp = ves.blockDistancePrev();
                int bd = ves.blockDistance();
                if (bdp == bd) continue;
                Vec3 newPosF = ves.position();
                Vec3 oldPosF = newPosF.subtract(ves.motion);
                BlockPos oldPos = BlockPos.containing(oldPosF);
                BlockPos newPos = BlockPos.containing(newPosF);
                int id1 = Math.clamp(bdp, 0, blockStates.length - 1);
                int id2 = Math.clamp(bd, 0, blockStates.length - 1);
                ves.onRunIntoNewBlock(oldPos, blockStates[id1], newPos, blockStates[id2], shapes[id2]);
            }
        }
    }

    private boolean hitEntityForDisplayTime(VirtualEtherStream ves, Vec3 pos, List<Entity> entities) {
        for (Entity entity : entities) {
            if (entity.getBoundingBox().inflate(0.3).contains(pos)) return true;
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

    private void commonHitBlock(BlockHitResult blockHit, VirtualEtherStream ves) {
        boolean handled = false;
        BlockState hitBlockState = level.getBlockState(blockHit.getBlockPos());
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

    private void markBlockSkipped(BlockPos blockPos) {
        if (cachedBlockStates == null || cachedBlockPoses == null) return;
        int index = blockPos.get(direction.getAxis()) - pos.get(direction.getAxis());
        if (direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            index = -index;
        }
        if (index < 0 || index >= cachedBlockStates.length) return;
        cachedSkip[index] = true;
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

    private @Nullable EntityHitResult collideTryEntity(VirtualEtherStream ves, List<Entity> entities, double blockDist, Vec3 oldPos, Vec3 newPos) {
        Entity hitEntity = null;
        Vec3 entityHitAt = null;
        double nearestDist = blockDist;
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
        EntityHitResult hit = null;
        if (hitEntity != null) {
            hit = new EntityHitResult(hitEntity, entityHitAt);
        }
        return hit;
    }

    private @Nullable BlockHitResult collideTryBlock(VirtualEtherStream ves, BlockState[] blockStates, BlockPos[] blockPoses, boolean[] skip, VoxelShape[] shapes, int clipStart, int clipEnd, Vec3 oldPos, Vec3 newPos) {
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
            BlockHitResult hit = shapes[j].clip(oldPos, newPos, pos);
            if (hit != null) {
                return hit;
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
