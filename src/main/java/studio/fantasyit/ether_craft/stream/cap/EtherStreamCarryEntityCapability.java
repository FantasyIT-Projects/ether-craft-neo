package studio.fantasyit.ether_craft.stream.cap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

import javax.annotation.Nullable;
import java.util.UUID;

public class EtherStreamCarryEntityCapability implements IStreamCapability {
    public static final Identifier ID = EtherCraft.id("carry_entity");

    public @Nullable UUID carriedUUID;
    public @Nullable Entity carriedEntity;

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void tick(IEtherStreamLike streamEntity) {
        if (carriedEntity != null) {
            carriedEntity.noPhysics = true;
            carriedEntity.setPos(streamEntity.position());
            Vec3 vec3 = streamEntity.deltaMovement();
            carriedEntity.setDeltaMovement(vec3);
        }
    }

    @Override
    public boolean hitEntity(ServerLevel level, IEtherStreamLike streamEntity, EntityHitResult hit, Entity entity) {
        if (carriedUUID == null) {
            carriedUUID = entity.getUUID();
            carriedEntity = entity;
            return true;
        }
        if (carriedUUID.equals(entity.getUUID())) {
            carriedUUID = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean hitBlock(ServerLevel level, IEtherStreamLike streamEntity, BlockHitResult hit, BlockState blockState) {
        return false;
    }

    @Override
    public void onDestroy(IEtherStreamLike streamEntity) {
        if (carriedEntity != null && carriedEntity.isAlive()) {
            BlockPos pos = streamEntity.blockPosition().below();
            if (streamEntity.level().getBlockState(pos).isEmpty())
                carriedEntity.teleportTo(pos.getX(), pos.getY(), pos.getZ());
            else
                carriedEntity.teleportTo(streamEntity.position().x, streamEntity.position().y, streamEntity.position().z);
        }
    }


    @Override
    public boolean shouldPassThrough(Entity entity) {
        return entity.getUUID().equals(carriedEntity);
    }

    @Override
    public void serialize(ValueOutput output) {
    }

    @Override
    public void deserialize(ValueInput input) {
    }
}
