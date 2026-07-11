package studio.fantasyit.ether_craft.stream.cap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.register.Tags;
import studio.fantasyit.ether_craft.stream.EtherConsumer;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.data.EtherStreamCarryingEntityData;

import java.util.Optional;

public class EtherStreamCarryEntityCapability implements IStreamCapability {
    public static final Identifier ID = EtherCraft.id("carry_entity");
    public static final Identifier ID_PLAYER = EtherCraft.id("carry_player");

    public static final Codec<EtherStreamCarryEntityCapability> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("source").forGetter(t -> t.source),
            Codec.BOOL.optionalFieldOf("playerOnly", false).forGetter(t -> t.playerOnly)
    ).apply(instance, EtherStreamCarryEntityCapability::new));

    public static final Codec<EtherStreamCarryEntityCapability> CODEC_PLAYER = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("source").forGetter(t -> t.source),
            Codec.BOOL.optionalFieldOf("playerOnly", true).forGetter(t -> t.playerOnly)
    ).apply(instance, EtherStreamCarryEntityCapability::new));

    private Entity cachedEntity;

    private BlockPos source;
    private final boolean playerOnly;

    public EtherStreamCarryEntityCapability(BlockPos source) {
        this(source, false);
    }

    public EtherStreamCarryEntityCapability(BlockPos source, boolean playerOnly) {
        this.source = source;
        this.playerOnly = playerOnly;
    }

    @Override
    public Identifier getId() {
        return playerOnly ? ID_PLAYER : ID;
    }

    @Override
    public void tick(IEtherStreamLike streamEntity) {
        EtherStreamCarryingEntityData data = getCarriedData(streamEntity);
        if (data == null) {
            cachedEntity = null;
            return;
        }

        if (cachedEntity != null && cachedEntity.getUUID().equals(data.entityUUID())) {
            if (cachedEntity instanceof ServerPlayer sp && sp.isShiftKeyDown()) {
                EtherStreamCarryEntityCapability.dropEntityTo(sp.level(), streamEntity.position(), streamEntity.deltaMovement(), sp);
                sp.setData(AttachmentDataRegistry.CARRY_COOLDOWN.get(), sp.level().getGameTime());
                sp.setData(AttachmentDataRegistry.CARRY_COOLDOWN_SOURCE.get(), Optional.empty());
                streamEntity.clearSyncedData(EtherStreamCarryingEntityData.ID);
                return;
            }
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
        Vec3 position = streamEntity.position();
        cachedEntity.setPos(position.x, position.y - cachedEntity.getEyeHeight(), position.z);
        cachedEntity.setDeltaMovement(streamEntity.deltaMovement());
        cachedEntity.setInvisible(true);
    }

    @Override
    public void getConsumption(EtherConsumer consumer, IEtherStreamLike entity) {
        EtherStreamCarryingEntityData data = getCarriedData(entity);
        if (data != null)
            consumer.addBaseFactor(0.005f);
    }

    @Override
    public boolean hitEntity(ServerLevel level, IEtherStreamLike streamEntity, EntityHitResult hit, Entity entity) {
        if (entity.is(Tags.ETHER_STREAM_CANNOT_CARRY))
            return true;

        if (playerOnly && !(entity instanceof ServerPlayer))
            return true;

        EtherStreamCarryingEntityData data = getCarriedData(streamEntity);

        if (data == null) {
            long cooldown = entity.getData(AttachmentDataRegistry.CARRY_COOLDOWN.get());
            if (level.getGameTime() - cooldown < 40) {
                Optional<BlockPos> source = entity.getData(AttachmentDataRegistry.CARRY_COOLDOWN_SOURCE);
                if (source.isEmpty() || source.get().equals(this.source))
                    return false;
            }
            if (entity.hasData(AttachmentDataRegistry.TAKEN_BY_ETHER_STREAM) && entity.getData(AttachmentDataRegistry.TAKEN_BY_ETHER_STREAM))
                return true;

            if (entity.isVehicle()) {
                entity.ejectPassengers();
            }
            if (entity.isPassenger()) {
                entity.stopRiding();
            }

            streamEntity.setSyncedData(new EtherStreamCarryingEntityData(
                    entity.getUUID(), entity.getId(), streamEntity.getPosDir(), playerOnly, streamEntity.getStreamId()));
            cachedEntity = entity;
            entity.noPhysics = true;
            entity.setInvulnerable(true);
            entity.setData(AttachmentDataRegistry.TAKEN_BY_ETHER_STREAM, true);
            if (entity instanceof Player player) {
                player.setForcedPose(Pose.STANDING);
            }
            streamEntity.dirtyConsumer();
            if (cachedEntity instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.translatable("mount.onboard", Component.translatable("key.sneak")), true);
            }
            return true;
        }

        return data.entityUUID().equals(entity.getUUID());
    }

    public void forceTakeEntity(IEtherStreamLike streamEntity, Entity entity) {
        streamEntity.setSyncedData(new EtherStreamCarryingEntityData(
                entity.getUUID(), entity.getId(), streamEntity.getPosDir(), playerOnly, streamEntity.getStreamId()));
        cachedEntity = entity;
        entity.noPhysics = true;
        entity.setInvulnerable(true);
        entity.setData(AttachmentDataRegistry.TAKEN_BY_ETHER_STREAM, true);
        if (entity instanceof Player player) {
            player.setForcedPose(Pose.STANDING);
        }
        streamEntity.dirtyConsumer();
    }

    @Override
    public boolean hitBlock(ServerLevel level, IEtherStreamLike streamEntity, BlockHitResult hit, BlockState blockState) {
        return false;
    }

    @Override
    public void onDestroy(IEtherStreamLike streamEntity, @Nullable HitResult hitResult) {
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
            Vec3 dropPlayerPos = streamEntity.position();
            if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
                dropPlayerPos = hitResult.getLocation();
            }
            Vec3 motion = streamEntity.deltaMovement();
            dropEntityTo(streamEntity.level(), dropPlayerPos, motion, entity);
            entity.setData(AttachmentDataRegistry.CARRY_COOLDOWN.get(), entity.level().getGameTime());
            entity.setData(AttachmentDataRegistry.CARRY_COOLDOWN_SOURCE.get(), Optional.of(source));
        }
    }

    public static void dropEntityTo(Level level, Vec3 dropPlayerPos, Vec3 motion, Entity entity) {
        Vec3 subtract = dropPlayerPos.subtract(motion.normalize().scale(0.5));
        dropPlayerPos = new Vec3(Math.floor(subtract.x) + 0.5, dropPlayerPos.y, Math.floor(subtract.z) + 0.5);
        BlockPos currentBlockPos = BlockPos.containing((dropPlayerPos));
        int maxY = currentBlockPos.getY();
        int minY = (int) Math.floor(dropPlayerPos.y - entity.getEyeHeight());
        int suitableY = minY;
        for (int y = minY; y <= maxY; y++) {
            if (!level.getBlockState(new BlockPos(currentBlockPos.getX(), y - 1, currentBlockPos.getZ())).isEmpty()) {
                suitableY = y;
            }
        }
        if (suitableY == minY) {
            dropPlayerPos = dropPlayerPos.subtract(0, entity.getEyeHeight(), 0);
        } else {
            dropPlayerPos = new Vec3(dropPlayerPos.x, suitableY, dropPlayerPos.z);
        }
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0;
        entity.setInvulnerable(false);
        entity.setData(AttachmentDataRegistry.TAKEN_BY_ETHER_STREAM, false);
        if (entity.level() instanceof ServerLevel) {
            entity.teleportTo(dropPlayerPos.x, dropPlayerPos.y, dropPlayerPos.z);
            entity.setOldPosAndRot();
            entity.setInvisible(false);
        } else {
            entity.setPos(dropPlayerPos.x, dropPlayerPos.y, dropPlayerPos.z);
        }
        if (entity instanceof Player player) {
            player.setForcedPose(null);
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
