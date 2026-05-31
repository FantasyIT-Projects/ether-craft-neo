package studio.fantasyit.ether_craft.stream.vholder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamSetDyingS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamUpdateS2C;
import studio.fantasyit.ether_craft.register.Tags;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;

import java.util.*;

public class VirtualEtherStreamHolder {
    private final Direction direction;
    private final BlockPos pos;
    private ServerLevel level;
    int activateTick = 5;
    final List<VirtualEtherStream> streams = new ArrayList<>();
    int nextId = 0;

    public VirtualEtherStreamHolder(BlockPos pos, Direction direction, ServerLevel level) {
        this.level = level;
        this.pos = pos;
        this.direction = direction;
    }

    public VirtualEtherStream createStream(int ether, Vec3 pos, Vec3 motion) {
        VirtualEtherStream ves = new VirtualEtherStream(
                nextId++,
                ether,
                pos,
                motion,
                level,
                direction
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

    public void tick(PosDir posDir) {
        List<Integer> collectedToCreate = new ArrayList<>();
        List<Integer> collectedToRemove = new ArrayList<>();
        activateTick--;

        // === PER-VES TICK ===
        for (VirtualEtherStream ves : streams) {
            if (!level.isLoaded(ves.blockPosition())) continue;

            if (ves.consumer.isDirty()) {
                ves.consumer.recompute(ves.capabilities);
                ves.needsEtherSync = true;
            }

            ves.tickCount++;

            if (ves.tickCount == 1) {
                for (IStreamCapability cap : ves.capabilities) {
                    cap.firstTick(ves);
                }
            }

            if (ves.tickCount > Config.etherStreamMaxTick) {
                ves.markDead();
            }

            for (IStreamCapability cap : ves.capabilities) {
                cap.tick(ves);
            }

            int consumption = ves.getConsumption();
            ves.consumeEtherInternal(consumption);

            if (ves.getEther() <= 0) {
                ves.markDead();
            }
        }

        // === COLLISION ===
        // Compute furthest old pos and max motion length for entity query
        Vec3 emitterCenter = pos.getCenter();
        Vec3 furthestOldPos = emitterCenter;
        double furthestMotionLen = 0;
        for (VirtualEtherStream ves : streams) {
            if (ves.markToRemove) continue;
            if (ves.pos.distanceToSqr(emitterCenter) > furthestOldPos.distanceToSqr(emitterCenter)) {
                furthestOldPos = ves.pos;
            }
            double ml = ves.motion.length();
            if (ml > furthestMotionLen) {
                furthestMotionLen = ml;
            }
        }

        // Step A: Batch entity hit collection for all VES in this tick
        Map<VirtualEtherStream, List<EntityHitResult>> allEntityHits = new IdentityHashMap<>();
        for (VirtualEtherStream ves : streams) {
            if (ves.markToRemove) continue;
            allEntityHits.put(ves, new ArrayList<>());
        }

        double maxDist = Math.sqrt(furthestOldPos.distanceToSqr(pos.getCenter())) + furthestMotionLen;
        Vec3 queryVec = direction.getUnitVec3().scale(maxDist + 0.5);
        List<Entity> entities = level.getEntities(null, new AABB(pos).expandTowards(queryVec).inflate(1.0));
        for (Entity entity : entities) {
            if (entity == null) continue;
            AABB bb = entity.getBoundingBox().inflate(0.3);
            for (VirtualEtherStream ves : streams) {
                if (ves.markToRemove) continue;
                Vec3 oldPos = ves.pos;
                Vec3 newPos = oldPos.add(ves.motion);
                Optional<Vec3> clip = bb.clip(oldPos, newPos);
                clip.ifPresent(vec3 -> allEntityHits.get(ves).add(new EntityHitResult(entity, vec3)));
            }
        }

        // Step B: Per-VES collision resolution (block + entity, nearest)
        for (VirtualEtherStream ves : streams) {
            if (ves.markToRemove) continue;
            Vec3 oldPos = ves.pos;
            Vec3 newPos = oldPos.add(ves.motion);

            // Block hit
            BlockHitResult blockHit = level.clipIncludingBorder(
                    new ClipContext(oldPos, newPos,
                            ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE, CollisionContext.empty()));
            boolean hasBlockHit = blockHit.getType() != HitResult.Type.MISS;

            // Check pass-through
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

            // Nearest entity hit
            EntityHitResult nearestEntity = null;
            double nearestDist = Double.MAX_VALUE;
            for (EntityHitResult eh : allEntityHits.getOrDefault(ves, List.of())) {
                double d = oldPos.distanceToSqr(eh.getLocation());
                if (d < nearestDist) {
                    nearestDist = d;
                    nearestEntity = eh;
                }
            }

            // Resolve nearest
            if (blockDist < nearestDist && hasBlockHit) {
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
            } else if (nearestEntity != null) {
                boolean handled = false;
                for (IStreamCapability cap : ves.capabilities) {
                    handled |= cap.hitEntity(level, ves, nearestEntity, nearestEntity.getEntity());
                }
                if (!handled) ves.markDead();
            }

            // Move (even if dead - cleanup moves too)
            ves.pos = ves.pos.add(ves.motion);
        }

        // === COLLECT ===
        for (VirtualEtherStream ves : streams) {
            if (ves.markToRemove)
                collectedToRemove.add(ves.streamId);
            if (ves.markToSyncCreation) {
                collectedToCreate.add(ves.streamId);
                ves.markToSyncCreation = false;
            }
        }

        // === SYNC ===
        if (!collectedToCreate.isEmpty()) {
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
                            ves.label,
                            ves.labelColor
                    ));
                }
            }
            EtherStreamCreateS2C payload = new EtherStreamCreateS2C(posDir, createEntries);
            PacketDistributor.sendToPlayersInDimension(level, payload);
        }

        if (!collectedToRemove.isEmpty()) {
            List<Integer> entries = new ArrayList<>();
            for (VirtualEtherStream ves : streams) {
                if (ves.markToRemove) {
                    entries.add(ves.streamId);
                }
            }
            EtherStreamSetDyingS2C payload = new EtherStreamSetDyingS2C(posDir, entries);
            PacketDistributor.sendToPlayersInDimension(level, payload);
        }

        List<EtherStreamUpdateS2C.StreamEntry> updateEntries = new ArrayList<>();
        for (VirtualEtherStream ves : streams) {
            if (ves.markToRemove || ves.markToSyncCreation) continue;
            if (ves.needsEtherSync) {
                updateEntries.add(new EtherStreamUpdateS2C.StreamEntry(
                        ves.streamId,
                        ves.ether,
                        ves.consumer.toState()
                ));
                ves.needsEtherSync = false;
            }
        }
        if (!updateEntries.isEmpty()) {
            EtherStreamUpdateS2C payload = new EtherStreamUpdateS2C(posDir, updateEntries);
            PacketDistributor.sendToPlayersInDimension(level, payload);
        }

        // === CLEANUP ===
        streams.removeIf(ves -> ves.markToRemove);
    }

    private VirtualEtherStream findStreamById(int id) {
        for (VirtualEtherStream ves : streams) {
            if (ves.streamId == id) return ves;
        }
        return null;
    }

    void syncStreamsToPlayer(ServerPlayer player, PosDir posDir) {
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
                    ves.label,
                    ves.labelColor
            ));
        }
        if (!entries.isEmpty()) {
            EtherStreamCreateS2C payload = new EtherStreamCreateS2C(posDir, entries);
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public boolean isDead() {
        return activateTick <= 0 && streams.isEmpty();
    }

    VirtualEtherStreamHolderData toData() {
        List<VirtualEtherStreamData> streamDataList = new ArrayList<>();
        for (VirtualEtherStream ves : streams) {
            if (!ves.markToRemove) {
                streamDataList.add(ves.toData());
            }
        }
        return new VirtualEtherStreamHolderData(activateTick, nextId, streamDataList);
    }

    void initLevel(ServerLevel level) {
        this.level = level;
        for (VirtualEtherStream ves : streams) {
            ves.level = level;
        }
    }
}
