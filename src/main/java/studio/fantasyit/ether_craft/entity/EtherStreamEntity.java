package studio.fantasyit.ether_craft.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.register.EntityRegistry;
import studio.fantasyit.ether_craft.register.Tags;
import studio.fantasyit.ether_craft.stream.IStreamCapability;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EtherStreamEntity extends Projectile {
    static final EntityDataAccessor<Integer> ETHER_COUNT = SynchedEntityData.defineId(EtherStreamEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<java.util.Optional<Component>> LABEL_DATA =
            SynchedEntityData.defineId(EtherStreamEntity.class, EntityDataSerializers.OPTIONAL_COMPONENT);
    public static final EntityDataAccessor<Vector3fc> START_POS =
            SynchedEntityData.defineId(EtherStreamEntity.class, EntityDataSerializers.VECTOR3);
    public static final EntityDataAccessor<Integer> LABEL_COLOR =
            SynchedEntityData.defineId(EtherStreamEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DYING =
            SynchedEntityData.defineId(EtherStreamEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Vector3fc> DEATH_POS =
            SynchedEntityData.defineId(EtherStreamEntity.class, EntityDataSerializers.VECTOR3);
    private int ether;
    private int lowerConsumeFactor = 0;
    public static final int MAX_TAIL = 6;
    public final double[] tailX = new double[MAX_TAIL];
    public final double[] tailY = new double[MAX_TAIL];
    public final double[] tailZ = new double[MAX_TAIL];
    public final float[] tailSize = new float[MAX_TAIL];
    public int tailHead = -1;
    public int tailCount;
    public boolean ticked = false;
    public int clientDeathTick;
    private int deathTickStart;
    private static final int MAX_DEATH_TICKS = 60;
    private List<IStreamCapability> capabilities = new ArrayList<>();
    private ChainedEmitterEntityHitCache ceec;
    private ChainedEmitterEntityHitCache.PosDir posDir;

    public static EtherStreamEntity create(Level level, int ether, Vec3 position, Vec3 motion) {
        EtherStreamEntity instance = new EtherStreamEntity(EntityRegistry.ETHER_STREAM_ENTITY.get(), level);
        instance.ether = ether;
        instance.setPos(position);
        instance.setDeltaMovement(motion);
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
        builder.define(LABEL_DATA, java.util.Optional.empty());
        builder.define(START_POS, new Vector3f());
        builder.define(LABEL_COLOR, 0xFFFFFFFF);
        builder.define(DYING, false);
        builder.define(DEATH_POS, new Vector3f());
    }

    public void firstTick() {
        this.ticked = true;
        for (IStreamCapability capability : capabilities) {
            capability.firstTick(this);
        }
    }

    public void setLowerConsumeFactor(int factor) {
        lowerConsumeFactor = factor;
    }

    public int getEther() {
        return ether;
    }

    public void consumeEther(int amount) {
        this.ether -= (int) Math.ceil(amount / getLowerFactory());
        this.entityData.set(ETHER_COUNT, ether);
    }

    public void addCapability(IStreamCapability capability) {
        capabilities.add(capability);
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
        if (ceec == null) {
            ceec = level().getData(AttachmentDataRegistry.CHAINED_EMITTER_ENTITY_HIT_CACHE);
            Direction d = Direction.getApproximateNearest(getDeltaMovement());
            posDir = new ChainedEmitterEntityHitCache.PosDir(this.blockPosition(), d);
        }
        if (this.level().isClientSide()) {
            this.ether = this.entityData.get(ETHER_COUNT);
            if (entityData.get(DYING)) {
                clientDeathTick++;
            }
            tailHead = (tailHead + 1) % MAX_TAIL;
            if (tailCount < MAX_TAIL) tailCount++;
            tailX[tailHead] = this.getX();
            tailY[tailHead] = this.getY();
            tailZ[tailHead] = this.getZ();
            tailSize[tailHead] = getSize();
        } else {
            if (!ticked)
                firstTick();
            if (entityData.get(DYING)) {
                Vec3 vec3 = this.getDeltaMovement();
                this.setPos(this.getX() + vec3.x, this.getY() + vec3.y, this.getZ() + vec3.z);
                if (this.tickCount - deathTickStart > MAX_DEATH_TICKS) {
                    this.discard();
                }
                return;
            }
            if (this.tickCount >= Config.etherStreamMaxTick) {
                this.dropAndDiscard();
                return;
            }
            this.consumeEther(this.getConsumption());
            if (ether <= 0) {
                this.dropAndDiscard();
                return;
            }
        }

        for (IStreamCapability capability : capabilities) {
            capability.tick(this);
        }

        Vec3 vec3 = this.getDeltaMovement();
        HitResult hitresult = fastHit();
        if (hitresult.getType() != HitResult.Type.MISS)
            this.onHit(hitresult);
        this.setPos(this.getX() + vec3.x, this.getY() + vec3.y, this.getZ() + vec3.z);
    }


    private int getConsumption() {
        double factor = Config.etherStreamConsumptionFactor;
        factor += Config.etherStreamConsumptionByTimeFactor * this.tickCount;
        double value = Math.ceil(factor * ether);
        for (IStreamCapability capability : capabilities) {
            value += capability.getConsumption();
        }
        return (int) Math.ceil(value);
    }

    public double getLowerFactory() {
        return Math.pow(2, lowerConsumeFactor);
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
                dropAndDiscard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult p_37259_) {
        if (this.level().isClientSide()) return;
        Entity entity = p_37259_.getEntity();
        boolean handled = false;
        for (IStreamCapability capability : capabilities)
            if (capability.hitEntity((ServerLevel) level(), this, p_37259_, entity))
                handled = true;
        if (!handled)
            dropAndDiscard();
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
        return posDir.dir();
    }

    public void dropAndDiscard() {
        if (entityData.get(DYING)) return;
        for (IStreamCapability capability : capabilities) {
            capability.onDestroy(this);
        }
        if (entityData.get(LABEL_DATA).isPresent()) {
            deathTickStart = this.tickCount;
            entityData.set(DEATH_POS, new Vector3f((float) this.getX(), (float) this.getY(), (float) this.getZ()));
            entityData.set(DYING, true);
        } else {
            this.discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
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

        List<Entity> entities = null;
        if (ceec != null && posDir != null)
            entities = ceec.getAllEntities(this, posDir, 1, 2);
        if (entities == null)
            entities = level.getEntities(this, getBoundingBox().expandTowards(movement).inflate(1.0F), this::canHitEntity);

        double nearest = Double.MAX_VALUE;
        double entityMargin = ProjectileUtil.computeMargin(this);
        Optional<Vec3> nearestLocation = Optional.empty();
        Entity hitEntity = null;
        for (Entity entity : entities) {
            if (!this.canHitEntity(entity))
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
