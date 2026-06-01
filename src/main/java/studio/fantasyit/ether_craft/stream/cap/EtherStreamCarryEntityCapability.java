package studio.fantasyit.ether_craft.stream.cap;

import com.mojang.serialization.Codec;
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
import studio.fantasyit.ether_craft.stream.data.EtherStreamCarryingEntityData;
import studio.fantasyit.ether_craft.stream.vholder.VirtualEtherStream;

public class EtherStreamCarryEntityCapability implements IStreamCapability {
    public static final Identifier ID = EtherCraft.id("carry_entity");
    public static final Codec<EtherStreamCarryEntityCapability> CODEC =
            Codec.INT.xmap(i -> new EtherStreamCarryEntityCapability(), c -> 0);

    private transient Entity cachedEntity;

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void tick(IEtherStreamLike streamEntity) {
        EtherStreamCarryingEntityData data = getCarriedData(streamEntity);
        if (data == null) {
            cachedEntity = null;
            return;
        }

        if (cachedEntity == null || !cachedEntity.getUUID().equals(data.entityUUID())) {
            if (streamEntity.level() instanceof ServerLevel sl) {
                cachedEntity = sl.getEntity(data.entityUUID());
            }
        }

        if (cachedEntity == null || !cachedEntity.isAlive() || cachedEntity.isRemoved()
                || cachedEntity.level() != streamEntity.level()) {
            streamEntity.clearSyncedData(EtherStreamCarryingEntityData.ID);
            cachedEntity = null;
            return;
        }

        cachedEntity.noPhysics = true;
        cachedEntity.setPos(streamEntity.position());
        cachedEntity.setDeltaMovement(streamEntity.deltaMovement());
    }

    @Override
    public boolean hitEntity(ServerLevel level, IEtherStreamLike streamEntity, EntityHitResult hit, Entity entity) {
        EtherStreamCarryingEntityData data = getCarriedData(streamEntity);

        if (data == null) {
            if (streamEntity instanceof VirtualEtherStream ves) {
                streamEntity.setSyncedData(new EtherStreamCarryingEntityData(
                        entity.getUUID(), entity.getId(), ves.getPosDir(), ves.getStreamId()));
            }
            cachedEntity = entity;
            return true;
        }

        if (data.entityUUID().equals(entity.getUUID())) {
            return true;
        }

        streamEntity.clearSyncedData(EtherStreamCarryingEntityData.ID);
        cachedEntity = null;
        return false;
    }

    @Override
    public boolean hitBlock(ServerLevel level, IEtherStreamLike streamEntity, BlockHitResult hit, BlockState blockState) {
        return false;
    }

    @Override
    public void onDestroy(IEtherStreamLike streamEntity) {
        EtherStreamCarryingEntityData data = getCarriedData(streamEntity);
        if (data == null) return;

        Entity entity = null;
        if (cachedEntity != null && cachedEntity.getUUID().equals(data.entityUUID())) {
            entity = cachedEntity;
        } else if (streamEntity.level() instanceof ServerLevel sl) {
            entity = sl.getEntity(data.entityUUID());
        }
        cachedEntity = null;

        if (entity != null && entity.isAlive()) {
            entity.noPhysics = false;
            entity.setDeltaMovement(Vec3.ZERO);
            BlockPos below = streamEntity.blockPosition().below();
            if (streamEntity.level().getBlockState(below).isEmpty()) {
                entity.teleportTo(below.getX() + 0.5, below.getY(), below.getZ() + 0.5);
            } else {
                entity.teleportTo(streamEntity.position().x, streamEntity.position().y, streamEntity.position().z);
            }
        }
    }

    @Override
    public boolean shouldPassThrough(Entity entity) {
        return cachedEntity != null && cachedEntity.getUUID().equals(entity.getUUID());
    }

    @Override
    public void serialize(ValueOutput output) {
    }

    @Override
    public void deserialize(ValueInput input) {
    }

    private EtherStreamCarryingEntityData getCarriedData(IEtherStreamLike streamEntity) {
        return (EtherStreamCarryingEntityData) streamEntity.getSyncedData(EtherStreamCarryingEntityData.ID);
    }
}
