package studio.fantasyit.ether_craft.stream.vholder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamSetDyingS2C;
import studio.fantasyit.ether_craft.register.Tags;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VirtualEtherStreamHolder {
    private final Direction direction;
    private final BlockPos pos;
    private final ServerLevel level;
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

    public void tick(ChainedEmitterEntityHitCache cache, PosDir posDir) {
        List<Integer> collectedToCreate = new ArrayList<>();
        List<Integer> collectedToRemove = new ArrayList<>();
        activateTick--;

        // === PER-VES TICK ===
        for (VirtualEtherStream ves : streams) {
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
            ves.consumeEther(consumption);

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

        List<Entity> entities = cache.getAllEntities(level, furthestOldPos, posDir, 0, (float) furthestMotionLen);
        if (entities != null) {
            for (Entity entity : entities) {
                if (entity == null) continue;
                AABB bb = entity.getBoundingBox().inflate(0.3);
                for (VirtualEtherStream ves : streams) {
                    if (ves.markToRemove) continue;
                    Vec3 oldPos = ves.pos;
                    Vec3 newPos = oldPos.add(ves.motion);
                    Optional<Vec3> clip = bb.clip(oldPos, newPos);
                    if (clip.isPresent()) {
                        allEntityHits.get(ves).add(new EntityHitResult(entity, clip.get()));
                    }
                }
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
                            ClipContext.Fluid.NONE, (CollisionContext) null));
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
                if (d < nearestDist) { nearestDist = d; nearestEntity = eh; }
            }

            // Resolve nearest
            if (blockDist < nearestDist && hasBlockHit) {
                boolean handled = false;
                for (IStreamCapability cap : ves.capabilities) {
                    handled |= cap.hitBlock(level, ves, blockHit, level.getBlockState(blockHit.getBlockPos()));
                }
                if (!handled) ves.markDead();
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
            if (ves.markToSyncCreation)
                collectedToCreate.add(ves.streamId);
        }

        // === SYNC ===
        for (int id : collectedToCreate) {
            VirtualEtherStream ves = findStreamById(id);
            if (ves != null) {
                EtherStreamCreateS2C payload = new EtherStreamCreateS2C(
                        posDir,
                        ves.streamId,
                        ves.startPos,
                        ves.motion,
                        ves.ether,
                        ves.tickCount,
                        ves.label,
                        ves.labelColor
                );
                PacketDistributor.sendToPlayersTrackingChunk(level, level.getChunkAt(pos).getPos(), payload);
            }
        }

        if (!collectedToRemove.isEmpty()) {
            List<EtherStreamSetDyingS2C.StreamEntry> entries = new ArrayList<>();
            for (VirtualEtherStream ves : streams) {
                if (ves.markToRemove) {
                    entries.add(new EtherStreamSetDyingS2C.StreamEntry(
                            ves.streamId,
                            ves.tickCount,
                            ves.ether,
                            true,
                            ves.label != null,
                            ves.label,
                            ves.labelColor
                    ));
                }
            }
            EtherStreamSetDyingS2C payload = new EtherStreamSetDyingS2C(posDir, entries);
            PacketDistributor.sendToPlayersTrackingChunk(level, level.getChunkAt(pos).getPos(), payload);
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

    public boolean isDead() {
        return activateTick <= 0 && streams.isEmpty();
    }
}
