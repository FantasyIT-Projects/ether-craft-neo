package studio.fantasyit.ether_craft.entity.vholder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.stream.EtherStreamStorageCapability;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.IStreamCapability;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class VirtualEtherStream implements IEtherStreamLike {
    Vec3 pos;
    Level level;
    Direction direction;
    int ether;
    List<IStreamCapability> capabilities;
    int streamId;
    Vec3 startPos;
    Vec3 motion;
    int tickCount = 0;
    boolean dead = false;
    boolean dying = false;
    int deathTick = 0;
    int labelColor = 0xFFFFFFFF;
    @Nullable Component label;


    @Override
    public BlockPos blockPosition() {
        return BlockPos.containing(pos);
    }

    @Override
    public Vec3 position() {
        return pos;
    }

    @Override
    public Level level() {
        return level;
    }

    @Override
    public void consumeEther(int ether) {
        this.ether -= ether;
    }

    @Override
    public int getEther() {
        return ether;
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    @Override
    public Optional<IStreamCapability> getCapability(Identifier id) {
        return capabilities.stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    @Override
    public void addCapability(EtherStreamStorageCapability capability) {
        this.capabilities.add(capability);
    }

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
        List<Entity> entities = cache.getAllEntities(level, pos, posDir, 0, motionLen);
        if (entities != null) {
            for (Entity entity : entities) {
                if (!(entity instanceof LivingEntity)) continue;
                AABB hitbox = entity.getBoundingBox().inflate(0.3);
                if (!hitbox.clip(pos, newPos).isPresent()) continue;
                boolean handled = false;
                ServerLevel serverLevel = (ServerLevel) level;
                EntityHitResult hitResult = new EntityHitResult(entity, pos);
                for (IStreamCapability cap : capabilities) {
                    if (cap.hitEntity(serverLevel, this, hitResult, entity)) handled = true;
                }
                if (!handled) {
                    markDead();
                    return;
                }
            }
        }

        BlockPos newBlockPos = BlockPos.containing(newPos);
        var blockState = level.getBlockState(newBlockPos);
        ServerLevel serverLevel = (ServerLevel) level;
        for (IStreamCapability cap : capabilities) {
            if (cap.shouldPassThrough(blockState, serverLevel, newBlockPos)) return;
        }
        boolean handled = false;
        BlockHitResult blockHitResult = new BlockHitResult(pos, direction.getOpposite(), newBlockPos, false);
        for (IStreamCapability cap : capabilities) {
            if (cap.hitBlock(serverLevel, this, blockHitResult, blockState)) handled = true;
        }
        if (!handled) markDead();
    }

    public void setStartData(Vec3 startPos, Vec3 motion) {
        this.startPos = startPos;
        this.motion = motion;
    }

    public void setLabel(@Nullable Component label, int color) {
        this.label = label;
        this.labelColor = color;
    }
}
