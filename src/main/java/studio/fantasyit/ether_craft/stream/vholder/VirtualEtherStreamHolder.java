package studio.fantasyit.ether_craft.stream.vholder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamSetDyingS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamSyncDataS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamUpdateS2C;
import studio.fantasyit.ether_craft.plating.helper.PlatingChargingUtil;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.register.BlockRegistry;
import studio.fantasyit.ether_craft.register.Tags;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VirtualEtherStreamHolder {
    private final Direction direction;
    private final BlockPos pos;
    private final PosDir posDir;
    private final ServerLevel level;
    final List<VirtualEtherStream> streams = new ArrayList<>();
    int nextId = 0;

    public VirtualEtherStreamHolder(PosDir posDir, @NotNull ServerLevel level) {
        this.level = level;
        this.pos = posDir.pos();
        this.direction = posDir.dir();
        this.posDir = posDir;
    }

    public VirtualEtherStream createStream(int ether, Vec3 pos, Vec3 motion) {
        VirtualEtherStream ves = new VirtualEtherStream(
                nextId++,
                ether,
                pos,
                posDir,
                motion,
                level,
                this
        );
        streams.add(ves);
        return ves;
    }

    public boolean hasStreamInUnloadedChunk() {
        for (VirtualEtherStream ves : streams) {
            if (ves.markToRemove) continue;
            BlockPos streamBlockPos = ves.blockPosition();
            if (!level.isLoaded(streamBlockPos)) {
                return true;
            }
        }
        return false;
    }

    public void tick() {
        for (int i = 0, size = streams.size(); i < size; i++) streams.get(i).tick();
        tickCollideAll();
        for (VirtualEtherStream ves : streams) {
            if (!ves.markToRemove)
                ves.pos = ves.pos.add(ves.motion);
        }
        syncAll();
        streams.removeIf(ves -> ves.markToRemove);
    }

    private void tickCollideAll() {
        if (streams.isEmpty()) return;
        Vec3 emitterBlockCenter = pos.getCenter();
        double maxLen = 0;
        double minLen = Double.MAX_VALUE;
        for (VirtualEtherStream ves : streams) {
            double lenOld = ves.pos.distanceTo(emitterBlockCenter);
            double lenNext = lenOld + ves.motion.length();
            if (lenNext > maxLen) maxLen = lenNext;
            if (lenOld < minLen) minLen = lenOld;
        }
        Vec3 queryVec = direction.getUnitVec3().scale(maxLen + 0.5);
        List<Entity> entities = level.getEntities(null, new AABB(pos).expandTowards(queryVec).inflate(1.0));
        entities.removeIf(entity -> entity == null || (entity.is(EntityType.ITEM) && !isPlatedItem((ItemEntity) entity)));
        int maxClipDist = (int) Math.ceil(maxLen);
        List<BlockState> blockStates = new ArrayList<>(maxClipDist);
        List<BlockPos> blockPoses = new ArrayList<>(maxClipDist);
        List<VoxelShape> shapes = new ArrayList<>(maxClipDist);

        BlockPos.MutableBlockPos blockScanPos = pos.mutable();
        for (int i = 0; i <= maxClipDist; i++) {
            BlockState blockState = level.getBlockState(blockScanPos);
            blockStates.add(blockState);
            blockPoses.add(blockScanPos.immutable());
            shapes.add(blockState.getCollisionShape(level, blockScanPos));
            blockScanPos.move(direction);
        }

        for (int i = 0, size = streams.size(); i < size; i++) {
            VirtualEtherStream ves = streams.get(i);
            Vec3 oldPos = ves.pos;
            Vec3 newPos = oldPos.add(ves.motion);

            int clipStart = BlockPos.containing(oldPos).distManhattan(pos);
            int clipEnd = BlockPos.containing(newPos).distManhattan(pos);
            //获取最近的方块碰撞
            BlockHitResult blockHit = null;
            for (int j = clipStart; j <= clipEnd; j++) {
                BlockState blockState = blockStates.get(j);
                BlockPos pos = blockPoses.get(j);
                if (blockState.is(Tags.ETHER_STREAM_PASS_THROUGH))
                    continue;
                if (ves.capabilities.stream().anyMatch(cap -> cap.shouldPassThrough(blockState, level, pos)))
                    continue;
                VoxelShape shape = shapes.get(j);
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
                if (hitEntity instanceof ItemEntity ie && isPlatedItem(ie)) {
                    addEtherToPlatedItem(ves, ie);
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
            if(ves.markToSyncCreation || ves.markToRemove) continue;
            BlockPos oldPos = BlockPos.containing(ves.pos);
            BlockPos newPos = BlockPos.containing(ves.pos.add(ves.motion));
            if (oldPos.equals(newPos)) continue;
            int id1 = oldPos.distManhattan(pos);
            int id2 = newPos.distManhattan(pos);
            boolean isEtherGlass1 = blockStates.get(id1).is(BlockRegistry.ETHER_GLASS);
            boolean isEtherGlass2 = blockStates.get(id2).is(BlockRegistry.ETHER_GLASS);
            if (isEtherGlass1 != isEtherGlass2) {
                ves.setRunIntoEtherGlass(isEtherGlass2);
            }
        }
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
                if (ves.needsEtherSync) {
                    collectedToSyncEtherConsume.add(ves.streamId);
                    ves.needsEtherSync = false;
                }
            }
        }

        if (!collectedToCreate.isEmpty()) {
            //创建新的Stream
            List<EtherStreamCreateS2C.StreamEntry> createEntries = new ArrayList<>();
            for (int id : collectedToCreate) {
                VirtualEtherStream ves = findStreamById(id);
                if (ves != null) {
                    if (ves.consumer.isDirty()) {
                        ves.consumer.recompute(ves, ves.capabilities);
                    }
                    createEntries.add(new EtherStreamCreateS2C.StreamEntry(
                            ves.streamId,
                            ves.startPos,
                            ves.motion,
                            ves.ether,
                            ves.tickCount,
                            ves.consumer.toState(),
                            ves.toSyncData
                    ));
                }
            }
            EtherStreamCreateS2C payload = new EtherStreamCreateS2C(posDir, createEntries);
            PacketDistributor.sendToPlayersInDimension(level, payload);
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
            PacketDistributor.sendToPlayersInDimension(level, payload);
        }


        if (!collectedToSyncData.isEmpty()) {
            //Synced data 变化的
            for (int id : collectedToSyncData) {
                VirtualEtherStream ves = findStreamById(id);
                if (ves == null) continue;
                EtherStreamSyncDataS2C payload = new EtherStreamSyncDataS2C(posDir, id, ves.toSyncData);
                PacketDistributor.sendToPlayersInDimension(level, payload);
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
                        ves.consumer.toState()
                );
                updateEntries.add(streamEntry);
            }
            if (!updateEntries.isEmpty()) {
                EtherStreamUpdateS2C payload = new EtherStreamUpdateS2C(posDir, updateEntries);
                PacketDistributor.sendToPlayersInDimension(level, payload);
            }
        }
    }

    public VirtualEtherStream findStreamById(int id) {
        for (VirtualEtherStream ves : streams) {
            if (ves.streamId == id) return ves;
        }
        return null;
    }

    void syncToPlayer(ServerPlayer player) {
        List<EtherStreamCreateS2C.StreamEntry> entries = new ArrayList<>();
        for (int i = 0, size = streams.size(); i < size; i++) {
            VirtualEtherStream ves = streams.get(i);
            if (ves.markToRemove) continue;
            if (ves.consumer.isDirty()) {
                ves.consumer.recompute(ves, ves.capabilities);
            }
            entries.add(new EtherStreamCreateS2C.StreamEntry(
                    ves.streamId,
                    ves.startPos,
                    ves.motion,
                    ves.ether,
                    ves.tickCount,
                    ves.consumer.toState(),
                    new ArrayList<>(ves.toSyncData)
            ));
        }
        if (!entries.isEmpty()) {
            EtherStreamCreateS2C payload = new EtherStreamCreateS2C(posDir, entries);
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public boolean isDead() {
        return streams.isEmpty();
    }

    private static boolean isPlatedItem(ItemEntity ie) {
        ItemStack stack = ie.getItem();
        return PlatingUtil.isPlatingInProgress(stack) || PlatingUtil.hasPlating(stack);
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
