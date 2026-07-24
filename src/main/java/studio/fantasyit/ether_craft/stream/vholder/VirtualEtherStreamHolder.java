package studio.fantasyit.ether_craft.stream.vholder;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.network.s2c.*;
import studio.fantasyit.ether_craft.plating.helper.PlatingChargingUtil;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.register.ItemRegistry;
import studio.fantasyit.ether_craft.register.Tags;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;
import studio.fantasyit.ether_craft.util.LevelUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class VirtualEtherStreamHolder {
    private final Direction direction;
    private final BlockPos pos;
    private final PosDir posDir;
    private final ServerLevel level;
    final List<VirtualEtherStream> streams = new ArrayList<>();
    private final Vec3i chunkVec;
    Int2IntOpenHashMap trackingPlayers = new Int2IntOpenHashMap();
    int nextId = 0;
    private boolean lastHadStreamInUnloadedChunk = false;

    public VirtualEtherStreamHolder(PosDir posDir, @NotNull ServerLevel level) {
        this.level = level;
        this.pos = posDir.pos();
        this.direction = posDir.dir();
        this.posDir = posDir;
        chunkVec = posDir.dir().getUnitVec3i().multiply(16);
    }

    public VirtualEtherStream createStream(int ether, float offset, float speed) {
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

    private int computeMaxBlockDist() {
        if (streams.isEmpty()) return 0;
        double maxDist = 0;
        switch (direction) {
            case UP, DOWN -> {
                double base = pos.getY() + 0.5;
                for (var ves : streams) {
                    if (ves.markToRemove) continue;
                    double d = Math.abs(ves.pos.y - base) + Math.abs(ves.motion.y);
                    if (d > maxDist) maxDist = d;
                }
            }
            case WEST, EAST -> {
                double base = pos.getX() + 0.5;
                for (var ves : streams) {
                    if (ves.markToRemove) continue;
                    double d = Math.abs(ves.pos.x - base) + Math.abs(ves.motion.x);
                    if (d > maxDist) maxDist = d;
                }
            }
            case NORTH, SOUTH -> {
                double base = pos.getZ() + 0.5;
                for (var ves : streams) {
                    if (ves.markToRemove) continue;
                    double d = Math.abs(ves.pos.z - base) + Math.abs(ves.motion.z);
                    if (d > maxDist) maxDist = d;
                }
            }
        }
        return (int) Math.ceil(maxDist);
    }

    public void tick() {
        if (streams.isEmpty()) return;
        int maxBlockDist = computeMaxBlockDist();
        lastHadStreamInUnloadedChunk = hasStreamInUnloadedChunk(maxBlockDist);
        if (lastHadStreamInUnloadedChunk) return;

        for (int i = 0, size = streams.size(); i < size; i++) {
            streams.get(i).tick();
        }
        tickCollideAll(maxBlockDist);
        for (VirtualEtherStream ves : streams) {
            if (!ves.markToRemove)
                ves.pos = ves.pos.add(ves.motion);
        }
        mergeAll();
        syncAll();
        updateTracking();
        streams.removeIf(ves -> ves.markToRemove);
    }

    private void mergeAll() {
        if (streams.isEmpty()) return;
        int maxLen = 0;
        for (VirtualEtherStream ves : streams) {
            maxLen = Math.max(maxLen, pos.distManhattan(ves.blockPosition()));
        }
        int size = maxLen + 1;
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

    private void tickCollideAll(int maxBlockDist) {
        int maxClipDist = maxBlockDist + 1;
        Vec3 queryVec = direction.getUnitVec3().scale(maxBlockDist + 1);
        List<Entity> entities = level.getEntities(null, new AABB(pos).expandTowards(queryVec).inflate(1.0));
        entities.removeIf(entity -> entity == null || (entity.is(EntityType.ITEM) && !PlatingUtil.isPlatedItemEntity((ItemEntity) entity) && !((ItemEntity) entity).getItem().is(Items.GLASS)));
        List<BlockState> blockStates = new ArrayList<>(maxClipDist);
        List<BlockPos> blockPoses = new ArrayList<>(maxClipDist);
        List<VoxelShape> shapes = new ArrayList<>(maxClipDist);
        List<Boolean> isPassThrough = new ArrayList<>(maxClipDist);

        BlockPos.MutableBlockPos blockScanPos = pos.mutable();
        for (int i = 0; i <= maxClipDist; i++) {
            BlockState blockState = level.getBlockState(blockScanPos);
            blockStates.add(blockState);
            blockPoses.add(blockScanPos.immutable());
            shapes.add(blockState.getCollisionShape(level, blockScanPos));
            isPassThrough.add(!blockState.isAir() && blockState.is(Tags.ETHER_STREAM_PASS_THROUGH));
            blockScanPos.move(direction);
        }

        for (int i = 0, size = streams.size(); i < size; i++) {
            VirtualEtherStream ves = streams.get(i);
            if (ves.markToRemove) continue;
            Vec3 oldPos = ves.pos;
            Vec3 newPos = oldPos.add(ves.motion);

            int clipStart = Math.clamp(BlockPos.containing(oldPos).distManhattan(pos), 0, blockStates.size() - 1);
            int clipEnd = Math.clamp(BlockPos.containing(newPos).distManhattan(pos), 0, blockStates.size() - 1);
            //获取最近的方块碰撞
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
            double blockDist = blockHit != null ? oldPos.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;

            //判断必方块更近的实体碰撞
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


            if (hitEntity != null) {
                boolean handled = false;
                EntityHitResult hit = new EntityHitResult(hitEntity, entityHitAt);
                if (hitEntity instanceof ItemEntity ie && PlatingUtil.isPlatedItemEntity(ie)) {
                    addEtherToPlatedItem(ves, ie);
                } else if (hitEntity instanceof ItemEntity ie && ie.getItem().is(Items.GLASS)) {
                    ie.setItem(new ItemStack(ItemRegistry.ETHER_GLASS_ITEM, ie.getItem().getCount()));
                } else {
                    for (IStreamCapability cap : ves.capabilities) {
                        handled |= cap.hitEntity(level, ves, hit, hitEntity);
                    }
                }
                if (!handled) {
                    PlatingChargingUtil.tryChargeEntity(ves, hitEntity);
                    ves.markDead(hit);
                }
            } else if (blockHit != null) {
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

    private void updateTracking() {
        for (VirtualEtherStream ves : streams) {
            if (ves.markToRemove || ves.trackingDirty) {
                for (int i : ves.trackingPlayers)
                    trackingPlayers.put(i, trackingPlayers.get(i) - 1);
            }
            if (ves.trackingInitial || ves.trackingDirty) {
                for (int i : ves.trackingPlayers)
                    trackingPlayers.put(i, trackingPlayers.get(i) + 1);
                ves.trackingInitial = false;
            }
            ves.trackingDirty = false;
        }
        trackingPlayers.int2IntEntrySet().removeIf(e -> e.getIntValue() <= 0);
    }

    private void syncAll() {
        List<Integer> collectedToCreate = new ArrayList<>();
        List<Integer> collectedToRemove = new ArrayList<>();
        List<Integer> collectedToSyncData = new ArrayList<>();
        List<Integer> collectedToSyncEtherConsume = new ArrayList<>();
        for (VirtualEtherStream ves : streams) {
            if (ves.markToRemove)
                collectedToRemove.add(ves.streamId);
            else if (ves.markToSyncCreation) {
                collectedToCreate.add(ves.streamId);
                ves.markToSyncCreation = false;
                ves.markToSyncData = false;
            } else {
                if (ves.markToSyncData) {
                    collectedToSyncData.add(ves.streamId);
                    ves.markToSyncData = false;
                }
                if (ves.needsEtherSync && ves.ether > 0) {
                    collectedToSyncEtherConsume.add(ves.streamId);
                    ves.needsEtherSync = false;
                }
            }
        }
        IntSet tracking = trackingPlayers.keySet();

        if (!collectedToCreate.isEmpty()) {
            //创建新的Stream
            for (int id : collectedToCreate) {
                VirtualEtherStream ves = findStreamById(id);
                if (ves != null) {
                    if (ves.consumer.isDirty()) {
                        ves.consumer.recompute(ves, ves.capabilities);
                    }
                    EtherStreamInitialCreateS2C etherStreamCreateS2C = new EtherStreamInitialCreateS2C(
                            posDir,
                            ves.streamId,
                            ves.startOffset,
                            ves.startSpeed,
                            ves.ether,
                            ves.consumer.toState(),
                            ves.toSyncData
                    );
                    sendToTrackingPlayers(level, ves.trackingPlayers, etherStreamCreateS2C);
                }
            }
        }

        if (!collectedToRemove.isEmpty()) {
            //删除Stream
            List<Integer> entries = new ArrayList<>();
            for (VirtualEtherStream ves : streams) {
                if (ves.markToRemove) {
                    entries.add(ves.streamId);
                }
            }
            EtherStreamSetDyingS2C payload = new EtherStreamSetDyingS2C(posDir, entries);
            sendToTrackingPlayers(level, tracking, payload);
        }


        if (!collectedToSyncData.isEmpty()) {
            //Synced data 变化的
            for (int id : collectedToSyncData) {
                VirtualEtherStream ves = findStreamById(id);
                if (ves == null) continue;
                EtherStreamSyncDataS2C payload = new EtherStreamSyncDataS2C(posDir, id, ves.toSyncData);
                sendToTrackingPlayers(level, tracking, payload);
            }
        }

        if (!collectedToSyncEtherConsume.isEmpty()) {
            //同步消耗对象
            List<EtherStreamUpdateS2C.StreamEntry> updateEntries = new ArrayList<>();
            for (int id : collectedToSyncEtherConsume) {
                VirtualEtherStream ves = findStreamById(id);
                if (ves == null) continue;
                EtherStreamUpdateS2C.StreamEntry streamEntry = new EtherStreamUpdateS2C.StreamEntry(
                        ves.streamId,
                        ves.ether,
                        ves.needsEtherConsumerSync ? Optional.of(ves.consumer.toState()) : Optional.empty()
                );
                ves.needsEtherConsumerSync = false;
                updateEntries.add(streamEntry);
            }
            if (!updateEntries.isEmpty()) {
                EtherStreamUpdateS2C payload = new EtherStreamUpdateS2C(posDir, updateEntries);
                sendToTrackingPlayers(level, tracking, payload);
            }
        }
    }

    private void sendToTrackingPlayers(ServerLevel level, Set<Integer> id, CustomPacketPayload payload) {
        if (Config.etherStreamSyncDistance <= 0) {
            PacketDistributor.sendToPlayersInDimension(level, payload);
        } else {
            level.getPlayers(t -> id.contains(t.getId())).forEach(p ->
                    PacketDistributor.sendToPlayer(p, payload)
            );
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
            EtherStreamBatchCreateS2C payload = new EtherStreamBatchCreateS2C(posDir, entries);
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public boolean isDead() {
        return streams.isEmpty();
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
    }
}
