package studio.fantasyit.ether_craft.stream.vholder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamSetDyingS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamSyncDataS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamUpdateS2C;
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
                level
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
        for (VirtualEtherStream ves : streams) ves.tick();
        tickCollideAll();
        for (VirtualEtherStream ves : streams) {
            if (!ves.markToRemove)
                ves.pos = ves.pos.add(ves.motion);
        }
        syncAll();
        streams.removeIf(ves -> ves.markToRemove);
    }

    private void tickCollideAll() {
        Vec3 emitterBlockCenter = pos.getCenter();
        double maxLen = 0;
        double minLen = Double.MAX_VALUE;
        for (VirtualEtherStream ves : streams) {
            double lenOld = ves.pos.distanceTo(emitterBlockCenter);
            double lenNext = lenOld + ves.motion.length();
            if (lenNext > maxLen) maxLen = lenNext;
            if (lenOld < minLen) minLen = lenOld;
        }
        Vec3 queryVec = direction.getUnitVec3().scale(maxLen - minLen + 0.5);
        List<Entity> entities = level.getEntities(null, new AABB(pos).expandTowards(queryVec).inflate(1.0));
        entities.removeIf(entity -> entity == null || entity.is(EntityType.ITEM));


        for (VirtualEtherStream ves : streams) {
            Vec3 oldPos = ves.pos;
            Vec3 newPos = oldPos.add(ves.motion);

            //获取最近的方块碰撞
            BlockHitResult blockHit = level.clipIncludingBorder(new ClipContext(oldPos, newPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            boolean hasBlockHit = blockHit.getType() != HitResult.Type.MISS;
            if (hasBlockHit) {
                BlockPos bp = blockHit.getBlockPos();
                BlockState bs = level.getBlockState(bp);
                if (bs.is(Tags.ETHER_STREAM_PASS_THROUGH)) {
                    hasBlockHit = false;
                } else {
                    for (IStreamCapability cap : ves.capabilities) {
                        if (cap.shouldPassThrough(bs, level, bp)) {
                            hasBlockHit = false;
                            break;
                        }
                    }
                }
            }
            double blockDist = hasBlockHit ? oldPos.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;

            //判断必方块更近的实体碰撞
            Entity hitEntity = null;
            Vec3 entityHitAt = null;
            double nearestDist = blockDist;
            for (Entity entity : entities) {
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
                for (IStreamCapability cap : ves.capabilities) {
                    handled |= cap.hitEntity(level, ves, hit, hitEntity);
                }
                if (!handled) ves.markDead();
            } else if (hasBlockHit) {
                boolean handled = false;
                for (IStreamCapability cap : ves.capabilities) {
                    handled |= cap.hitBlock(level, ves, blockHit, level.getBlockState(blockHit.getBlockPos()));
                }
                if (!handled) {
                    EtherContainer capability = level.getCapability(EtherContainer.ETHER_CONTAINER, blockHit.getBlockPos());
                    if (capability != null)
                        capability.receiveEther(ves.getEther());
                    ves.markDead();
                }
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
            if (ves.markToSyncCreation) {
                collectedToCreate.add(ves.streamId);
                ves.markToSyncCreation = false;
                ves.markToSyncData = false;
            } else if (ves.markToSyncData) {
                collectedToSyncData.add(ves.streamId);
                ves.markToSyncData = false;
            } else if (ves.needsEtherSync) {
                collectedToSyncEtherConsume.add(ves.streamId);
                ves.needsEtherSync = false;
            }
        }

        if (!collectedToCreate.isEmpty()) {
            //创建新的Stream
            List<EtherStreamCreateS2C.StreamEntry> createEntries = new ArrayList<>();
            for (int id : collectedToCreate) {
                VirtualEtherStream ves = findStreamById(id);
                if (ves != null) {
                    if (ves.consumer.isDirty()) {
                        ves.consumer.recompute(ves.capabilities);
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
            for (int id : collectedToSyncData) {
                VirtualEtherStream ves = findStreamById(id);
                if (ves == null) continue;
                EtherStreamUpdateS2C.StreamEntry streamEntry = new EtherStreamUpdateS2C.StreamEntry(
                        ves.streamId,
                        ves.ether,
                        ves.consumer.toState()
                );
                updateEntries.add(streamEntry);
            }
            EtherStreamUpdateS2C payload = new EtherStreamUpdateS2C(posDir, updateEntries);
            PacketDistributor.sendToPlayersInDimension(level, payload);
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
        for (VirtualEtherStream ves : streams) {
            if (ves.markToRemove) continue;
            if (ves.consumer.isDirty()) {
                ves.consumer.recompute(ves.capabilities);
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

    VirtualEtherStreamHolderData toData() {
        List<VirtualEtherStreamData> streamDataList = new ArrayList<>();
        for (VirtualEtherStream ves : streams) {
            if (!ves.markToRemove) {
                streamDataList.add(ves.toData());
            }
        }
        return new VirtualEtherStreamHolderData(nextId, streamDataList);
    }


    public void loadFromData(VirtualEtherStreamHolderData holderData) {
        nextId = holderData.nextId();
        for (VirtualEtherStreamData data : holderData.streams()) {
            VirtualEtherStream ves = VirtualEtherStream.fromData(level, data);
            streams.add(ves);
        }
    }
}
