package studio.fantasyit.ether_craft.entity.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.register.BlockRegistry;
import studio.fantasyit.ether_craft.register.EntityDataSerializerRegistry;
import studio.fantasyit.ether_craft.register.EntityRegistry;
import studio.fantasyit.ether_craft.register.Tags;
import studio.fantasyit.ether_craft.stream.EtherConsumer;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;
import studio.fantasyit.ether_craft.stream.client.data.EntityStreamClientManager;
import studio.fantasyit.ether_craft.stream.data.EtherStreamLabelData;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;
import studio.fantasyit.ether_craft.stream.data.SyncedEtherStreamDataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EtherStreamEntity extends Projectile implements IEtherStreamLike {
    static int internalEtherId = 0;
    static final EntityDataAccessor<Integer> ETHER_COUNT = SynchedEntityData.defineId(EtherStreamEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<List<IEtherStreamSyncedData>> SYNCED_DATA =
            SynchedEntityData.defineId(EtherStreamEntity.class, EntityDataSerializerRegistry.SYNCED_DATA_LIST.get());
    private int ether;
    public static final int MAX_TAIL = 6;
    public final double[] tailX = new double[MAX_TAIL];
    public final double[] tailY = new double[MAX_TAIL];
    public final double[] tailZ = new double[MAX_TAIL];
    public final float[] tailSize = new float[MAX_TAIL];
    public int tailHead = -1;
    public int tailCount;
    private PosDir posDir;
    private List<IStreamCapability> capabilities = new ArrayList<>();
    public final EtherConsumer consumer = new EtherConsumer();
    private List<IEtherStreamSyncedData> toSyncData = new ArrayList<>();

    public static EtherStreamEntity create(Level level, int ether, Vec3 position, Vec3 motion) {
        EtherStreamEntity instance = new EtherStreamEntity(EntityRegistry.ETHER_STREAM_ENTITY.get(), level);
        instance.ether = ether;
        instance.entityData.set(ETHER_COUNT, ether);
        instance.setPos(position);
        instance.setDeltaMovement(motion);
        Direction approximateNearest = Direction.getApproximateNearest(motion);
        instance.posDir = new PosDir(BlockPos.containing(position), approximateNearest);
        return instance;
    }


    public EtherStreamEntity(EntityType<EtherStreamEntity> etherStreamEntityEntityType, Level level) {
        super(etherStreamEntityEntityType, level);
        this.ether = this.entityData.get(ETHER_COUNT);
    }

    protected float getSize() {
        return (float) (0.03 * Math.log10(this.ether));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ETHER_COUNT, ether);
        builder.define(SYNCED_DATA, new ArrayList<>());
    }

    public void firstTick() {
        for (IStreamCapability capability : capabilities) {
            capability.firstTick(this);
        }
    }

    public int getEther() {
        return ether;
    }

    @Override
    public Vec3 deltaMovement() {
        return getDeltaMovement();
    }

    @Override
    public void consumeEther(int amount) {
        consumeEtherInternal(amount);
        this.entityData.set(ETHER_COUNT, ether);
    }

    @Override
    public void consumeEtherInternal(int amount) {
        this.ether = Math.max(0, this.ether - amount);
    }

    @Override
    public void dirtyConsumer() {
        consumer.markDirty();
    }

    @Override
    public void addCapability(IStreamCapability capability) {
        capabilities.add(capability);
        capability.setConsumer(this.consumer);
    }

    public Optional<IStreamCapability> getCapability(Identifier id) {
        for (IStreamCapability capability : capabilities) {
            if (capability.getId().equals(id))
                return Optional.of(capability);
        }
        return Optional.empty();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.ether = this.entityData.get(ETHER_COUNT);
            List<IEtherStreamSyncedData> synced = this.entityData.get(SYNCED_DATA);
            if (synced != null) {
                this.toSyncData = new ArrayList<>(synced);
            }
            tailHead = (tailHead + 1) % MAX_TAIL;
            if (tailCount < MAX_TAIL) tailCount++;
            tailX[tailHead] = this.getX();
            tailY[tailHead] = this.getY();
            tailZ[tailHead] = this.getZ();
            tailSize[tailHead] = getSize();
        } else {
            if (!level().isLoaded(this.blockPosition())) {
                return;
            }

            if (consumer.isDirty()) {
                consumer.recompute(this, capabilities);
            }

            if (this.tickCount == 0)
                firstTick();

            for (IStreamCapability capability : capabilities) {
                capability.tick(this);
            }
            int consumption = consumer.getTotalConsumption(ether, tickCount);
            this.consumeEtherInternal(consumption);

            if (ether <= 0 || this.tickCount >= Config.etherStreamMaxTick) {
                this.dropAndDiscard(null);
                return;
            }
        }

        Vec3 vec3 = this.getDeltaMovement();
        if(!this.level().isClientSide()) {
            HitResult hitresult = fastHit();
            if (hitresult.getType() != HitResult.Type.MISS)
                this.onHit(hitresult);
        }
        this.setPos(this.getX() + vec3.x, this.getY() + vec3.y, this.getZ() + vec3.z);

        if (!this.level().isClientSide()) {
            BlockPos oldBlock = BlockPos.containing(this.getX() - vec3.x, this.getY() - vec3.y, this.getZ() - vec3.z);
            BlockPos newBlock = this.blockPosition();
            boolean wasGlass = level().getBlockState(oldBlock).is(BlockRegistry.ETHER_GLASS);
            boolean isGlass = level().getBlockState(newBlock).is(BlockRegistry.ETHER_GLASS);
            if (wasGlass != isGlass) {
                setRunIntoEtherGlass(isGlass);
            }
        }
    }


    @Override
    public boolean shouldPassThrough(Entity entity) {
        for (IStreamCapability cap : capabilities)
            if (cap.shouldPassThrough(entity))
                return true;
        return false;
    }

    @Override
    public void setSyncedData(IEtherStreamSyncedData data) {
        toSyncData.removeIf(d -> d.getId().equals(data.getId()));
        toSyncData.add(data);
        this.entityData.set(SYNCED_DATA, new ArrayList<>(toSyncData));
    }

    @Override
    public void clearSyncedData(Identifier id) {
        toSyncData.removeIf(d -> d.getId().equals(id));
        this.entityData.set(SYNCED_DATA, new ArrayList<>(toSyncData));
    }

    @Override
    public @Nullable IEtherStreamSyncedData getSyncedData(Identifier id) {
        for (IEtherStreamSyncedData d : toSyncData) {
            if (d.getId().equals(id))
                return d;
        }
        return null;
    }

    @Override
    public void setRunIntoEtherGlass(boolean isEtherGlass2) {
        consumer.setIsInEtherGlass(isEtherGlass2);
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (!this.level().isClientSide()) {
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hitResult;
                BlockState blockState = this.level().getBlockState(blockHit.getBlockPos());
                if (blockState.is(Tags.ETHER_STREAM_PASS_THROUGH)) {
                    return;
                }
                for (IStreamCapability cap : capabilities) {
                    if (cap.shouldPassThrough(blockState, (ServerLevel) level(), blockHit.getBlockPos())) {
                        return;
                    }
                }
            }
        } else {
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockhit = (BlockHitResult) hitResult;
                BlockState blockState = level().getBlockState(blockhit.getBlockPos());
                if (blockState.is(Tags.ETHER_STREAM_PASS_THROUGH)) {
                    return;
                }
            }
        }
        super.onHit(hitResult);
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult p_37258_) {
        if (!this.level().isClientSide()) {
            boolean handled = false;
            for (IStreamCapability capability : capabilities) {
                if (capability.hitBlock((ServerLevel) level(), this, p_37258_, level().getBlockState(p_37258_.getBlockPos())))
                    handled = true;
            }
            EtherContainer e = level().getCapability(EtherContainer.ETHER_CONTAINER, p_37258_.getBlockPos());
            if (e != null)
                e.receiveEther(this.ether);
            if (!handled)
                dropAndDiscard(p_37258_);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult p_37259_) {
        if (this.level().isClientSide()) return;
        Entity entity = p_37259_.getEntity();
        if (entity instanceof ItemEntity ie && (PlatingUtil.isPlatingInProgress(ie.getItem()) || PlatingUtil.hasPlating(ie.getItem()))) {
            int remaining = this.ether;
            if (remaining > 0) {
                ItemStack stack = ie.getItem();
                PlatingUtil.addEther(stack, Math.min(remaining, Config.platingMaxEtherReceive));
                consumeEther(remaining);
                ie.setItem(stack);
            }
            dropAndDiscard(p_37259_);
            return;
        }
        boolean handled = false;
        for (IStreamCapability capability : capabilities)
            if (capability.hitEntity((ServerLevel) level(), this, p_37259_, entity))
                handled = true;
        if (!handled)
            dropAndDiscard(p_37259_);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean collidedWithFluid(FluidState fluidState, BlockPos blockPos, Vec3 from, Vec3 to) {
        return false;
    }

    @Override
    protected boolean updateFluidInteraction() {
        return false;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket entityPacket) {
        super.recreateFromPacket(entityPacket);
        Vec3 d = entityPacket.getMovement();
        this.setDeltaMovement(d.x, d.y, d.z);
    }

    @Override
    public @NonNull Direction getDirection() {
        return Direction.getApproximateNearest(getDeltaMovement());
    }

    @Override
    public IEtherStreamLike recreate(Vec3 newMotion) {
        EtherStreamEntity newEntity = create(level(), this.ether, position(), newMotion);
        newEntity.capabilities = this.capabilities;
        this.capabilities = new ArrayList<>();
        for (IStreamCapability cap : newEntity.capabilities) {
            cap.setConsumer(newEntity.consumer);
        }
        newEntity.consumer.fromState(this.consumer.toState());
        newEntity.toSyncData = new ArrayList<>(this.toSyncData);
        newEntity.entityData.set(SYNCED_DATA, new ArrayList<>(this.toSyncData));
        for (IStreamCapability cap : newEntity.capabilities) {
            cap.onRecreate(newEntity);
        }
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.addFreshEntity(newEntity);
        }
        this.ether = 0;
        dropAndDiscard(null);
        return newEntity;
    }

    @Override
    public void removeInstantly() {
        dropAndDiscard(null);
    }

    @Override
    public int getStreamId() {
        return internalEtherId;
    }

    @Override
    public PosDir getPosDir() {
        return posDir;
    }

    public void dropAndDiscard(@Nullable HitResult hitResult) {
        for (IStreamCapability cap : capabilities) {
            if (!cap.onBeforeDestroy(this, hitResult)) return;
        }
        for (IStreamCapability capability : capabilities) {
            capability.onDestroy(this, hitResult);
        }
        if(level().isClientSide())
            EntityStreamClientManager.markDead(this);
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("toSyncData", SyncedEtherStreamDataManager.CODEC.listOf(), toSyncData);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        toSyncData = new ArrayList<>(input.read("toSyncData", SyncedEtherStreamDataManager.CODEC.listOf()).orElse(List.of()));
    }


    private HitResult fastHit() {
        Vec3 movement = getDeltaMovement();
        Level level = level();
        Vec3 from = position();
        Vec3 to = from.add(movement);
        HitResult hitResult = level.clipIncludingBorder(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hitResult.getType() != HitResult.Type.MISS) {
            to = hitResult.getLocation();
        }

        List<Entity> entities = level.getEntities(this, getBoundingBox().expandTowards(movement).inflate(1.0F), this::canHitEntity);

        double nearest = Double.MAX_VALUE;
        double entityMargin = ProjectileUtil.computeMargin(this);
        Optional<Vec3> nearestLocation = Optional.empty();
        Entity hitEntity = null;
        for (Entity entity : entities) {
            if (!this.canHitEntity(entity))
                continue;
            if (shouldPassThrough(entity))
                continue;
            AABB bb = entity.getBoundingBox().inflate(entityMargin);
            Optional<Vec3> location = bb.clip(from, to);
            if (location.isPresent()) {
                double dd = from.distanceToSqr(location.get());
                if (dd < nearest) {
                    hitEntity = entity;
                    nearest = dd;
                    nearestLocation = location;
                }
            }
        }

        return hitEntity == null ? hitResult : new EntityHitResult(hitEntity, nearestLocation.get());
    }
}
