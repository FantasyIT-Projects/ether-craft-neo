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
import studio.fantasyit.ether_craft.util.LevelUtil;

import java.util.*;

public class VirtualEtherStreamHolder {
    private final Direction direction;
    private final BlockPos pos;
    private final PosDir posDir;
    private final ServerLevel level;
    final List<VirtualEtherStream> streams = new ArrayList<>();
    private final Vec3i chunkVec;
    Int2IntOpenHashMap trackingPlayers = new Int2IntOpenHashMap();
    Int2IntOpenHashMap playerLastCreateId = new Int2IntOpenHashMap();
    int nextId = 0;
    private boolean lastHadStreamInUnloadedChunk = false;
    private int holderMaxDistance;
    CachedEtherStreamEntry lastCreateSnapshot = null;
    private int blockScanTickCounter = 0;
    private int cachedMaxClipDist = -1;
    private List<BlockState> cachedBlockStates;
    private List<BlockPos> cachedBlockPoses;
    private List<VoxelShape> cachedShapes;
    private List<Boolean> cachedIsPassThrough;


    public VirtualEtherStreamHolder(PosDir posDir, @NotNull ServerLevel level) {
        this.level = level;
        this.pos = posDir.pos();
        this.direction = posDir.dir();
        this.posDir = posDir;
        chunkVec = posDir.dir().getUnitVec3i().multiply(16);
    }

    private IndexMappingManager indexMappingManager() {
        return level.getData(AttachmentDataRegistry.INDEX_MAPPING_MANAGER);
    }

    private EntitySectorCache sectorCache;

    private EntitySectorCache sectorCache() {
        if (sectorCache == null) {
            sectorCache = VirtualEtherStreamHolderManager.get(level).sectorCache;
        }
        return sectorCache;
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
        return ves;
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
            streams.get(i).tick();
        }
        tickCollideAll(holderMaxDistance);
        mergeAll(holderMaxDistance);
        int nxtMaxDist = 0;
        for (VirtualEtherStream ves : streams) {
            if (!ves.markToRemove) {
                ves.pos = ves.pos.add(ves.motion);
                int distance = ves.blockPosition().distManhattan(posDir.pos()) + Mth.ceil(ves.startSpeed);
                if (distance > nxtMaxDist) {
                    nxtMaxDist = distance;
                }
            }
        }
        holderMaxDistance = nxtMaxDist + 1;
        collectSync(acc);
        updateNoLongerTracking();
        streams.removeIf(ves -> ves.markToRemove);
        ServerPerf.end(level);
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
        List<BlockState> blockStates = new ArrayList<>(maxClipDist + 1);
        List<BlockPos> blockPoses = new ArrayList<>(maxClipDist + 1);
        List<VoxelShape> shapes = new ArrayList<>(maxClipDist + 1);
        List<Boolean> isPassThrough = new ArrayList<>(maxClipDist + 1);
        BlockPos.MutableBlockPos blockScanPos = pos.mutable();
        for (int i = 0; i <= maxClipDist; i++) {
            BlockState blockState = level.getBlockState(blockScanPos);
            blockStates.add(blockState);
            blockPoses.add(blockScanPos.immutable());
            shapes.add(blockState.getCollisionShape(level, blockScanPos));
            isPassThrough.add(!blockState.isAir() && blockState.is(Tags.ETHER_STREAM_PASS_THROUGH));
            blockScanPos.move(direction);
        }
        cachedBlockStates = blockStates;
        cachedBlockPoses = blockPoses;
        cachedShapes = shapes;
        cachedIsPassThrough = isPassThrough;
    }

    private void tickCollideAll(int maxBlockDist) {
        int maxClipDist = maxBlockDist + 1;
        Vec3 queryVec = direction.getUnitVec3().scale(maxBlockDist + 1);
        List<Entity> allEntities = sectorCache().getEntities(level, new AABB(pos).expandTowards(queryVec).inflate(1.0));
        List<Entity> canHitEntity = new ArrayList<>(allEntities);
        canHitEntity.removeIf(this::entityNoCollidePredicator);
        ensureBlockSnapshot(maxClipDist);
        List<BlockState> blockStates = cachedBlockStates;
        List<BlockPos> blockPoses = cachedBlockPoses;
        List<VoxelShape> shapes = cachedShapes;
        List<Boolean> isPassThrough = cachedIsPassThrough;

        for (int i = 0, size = streams.size(); i < size; i++) {
            VirtualEtherStream ves = streams.get(i);
            if (ves.markToRemove) continue;
            Vec3 oldPos = ves.pos;
            Vec3 newPos = oldPos.add(ves.motion);

            int clipStart = Math.clamp(BlockPos.containing(oldPos).distManhattan(pos), 0, blockStates.size() - 1);
            int clipEnd = Math.clamp(BlockPos.containing(newPos).distManhattan(pos), 0, blockStates.size() - 1);
            //获取最近的方块碰撞
            BlockHitResult blockHit = collideTryBlock(ves, blockStates, blockPoses, isPassThrough, shapes, clipStart, clipEnd, oldPos, newPos);
            double blockDist = blockHit != null ? oldPos.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;
            //判断必方块更近的实体碰撞
            EntityHitResult entityHit = collideTryEntity(ves, canHitEntity, blockDist, oldPos);

            //确认将碰到entity
            if (entityHit != null) {
                commonHitEntity(entityHit, ves);
            } else if (blockHit != null) {
                commonHitBlock(blockHit, ves);
            }
        }

        for (int i = 0, size = streams.size(); i < size; i++) {
            VirtualEtherStream ves = streams.get(i);
            if (ves.markToSyncCreation || ves.markToRemove) continue;
            BlockPos oldPos = BlockPos.containing(ves.pos.subtract(ves.motion));
            BlockPos newPos = BlockPos.containing(ves.pos);
            if (oldPos.equals(newPos)) continue;
            int id1 = Math.clamp(oldPos.distManhattan(pos), 0, blockStates.size() - 1);
            int id2 = Math.clamp(newPos.distManhattan(pos), 0, blockStates.size() - 1);
            ves.onRunIntoNewBlock(oldPos, blockStates.get(id1), newPos, blockStates.get(id2));
        }
    }

    private boolean entityNoCollidePredicator(Entity entity) {
        if (entity instanceof ItemEntity ie) {
            if (PlatingUtil.isPlatedItemEntity(ie)) return false;
            if (ie.getItem().is(Items.GLASS)) return false;
            return true;
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
        if (!handled) {
            ves.markDead(blockHit);
        }
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

    private @Nullable EntityHitResult collideTryEntity(VirtualEtherStream ves, List<Entity> entities, double blockDist, Vec3 oldPos) {
        Entity hitEntity = null;
        Vec3 entityHitAt = null;
        double nearestDist = blockDist;
        for (Entity entity : entities) {
            if (entity.is(Tags.ETHER_STREAM_PASS_THROUGH_ENTITY))
                continue;
            AABB bb = entity.getBoundingBox().inflate(0.3);
            double localDist = entity.distanceToSqr(oldPos);
            boolean currentCanHit = bb.contains(ves.pos) && localDist < nearestDist;
            Vec3 localHitAt = bb.getCenter();
            if (!currentCanHit) {
                Vec3 oldEntityPos = ves.pos;
                Vec3 newEntityPos = oldPos.add(ves.motion);
                Optional<Vec3> clip = bb.clip(oldEntityPos, newEntityPos);
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

    private @Nullable BlockHitResult collideTryBlock(VirtualEtherStream ves, List<BlockState> blockStates, List<BlockPos> blockPoses, List<Boolean> isPassThrough, List<VoxelShape> shapes, int clipStart, int clipEnd, Vec3 oldPos, Vec3 newPos) {
        BlockHitResult blockHit = null;
        for (int j = clipStart; j <= clipEnd; j++) {
            BlockState blockState = blockStates.get(j);
            BlockPos pos = blockPoses.get(j);
            if (blockState.isAir()) continue;
            if (isPassThrough.get(j)) continue;
            boolean skip = false;
            for (IStreamCapability cap : ves.capabilities) {
                if (cap.shouldPassThrough(blockState, level, pos)) {
                    skip = true;
                    break;
                }
            }
            if (skip)
                continue;
            VoxelShape shape = shapes.get(j);
            if (shape.isEmpty()) continue;
            BlockHitResult hit = shape.clip(oldPos, newPos, pos);
            if (hit != null) {
                blockHit = hit;
                break;
            }
        }
        return blockHit;
    }

    private void updateTracking() {
        for (VirtualEtherStream ves : streams) {
            if (ves.trackingDirty) {
                for (int i : ves.lastTrackingPlayers)
                    trackingPlayers.addTo(i, -1);
            }
            if (ves.trackingInitial || ves.trackingDirty) {
                for (int i : ves.trackingPlayers)
                    trackingPlayers.addTo(i, 1);
                if (ves.trackingDirty) {
                    ves.lastTrackingPlayers = new IntOpenHashSet(ves.trackingPlayers);
                    ves.trackingDirty = false;
                }
                ves.trackingInitial = false;
            }
        }
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
