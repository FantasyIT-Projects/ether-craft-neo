package studio.fantasyit.ether_craft.entity;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.particle.ether_stream.EtherStreamData;
import studio.fantasyit.ether_craft.register.EntityRegistry;
import studio.fantasyit.ether_craft.register.Tags;
import studio.fantasyit.ether_craft.stream.IStreamCapability;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EtherStreamEntity extends Projectile {
    static final EntityDataAccessor<Integer> ETHER_COUNT = SynchedEntityData.defineId(EtherStreamEntity.class, EntityDataSerializers.INT);
    private int ether;
    private int lowerConsumeFactor = 0;
    public double[] renderTail = new double[30];
    public int renderTailIndex = 0;
    private List<IStreamCapability> capabilities = new ArrayList<>();

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
    }

    public void setLowerConsumeFactor(int factor) {
        lowerConsumeFactor = factor;
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
        if (this.level().isClientSide()) {
            this.ether = this.entityData.get(ETHER_COUNT);
            renderTail[(renderTailIndex++) % renderTail.length] = this.getX();
            renderTail[(renderTailIndex++) % renderTail.length] = this.getY();
            renderTail[(renderTailIndex++) % renderTail.length] = this.getZ();
        } else {
            if (this.tickCount >= Config.streamMaxTick) {
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
        HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitresult.getType() != HitResult.Type.MISS)
            this.onHit(hitresult);
        this.setPos(this.getX() + vec3.x, this.getY() + vec3.y, this.getZ() + vec3.z);
        if (this.level().isClientSide()) {
            this.level().addParticle(new EtherStreamData(0xffffff, this.getSize()), true, true, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        }
    }


    private int getConsumption() {
        double value = Math.ceil(0.002 * ether);
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

    public void dropAndDiscard() {
        for (IStreamCapability capability : capabilities) {
            capability.onDestroy(this);
        }
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
    }

    public void setChanged() {
        //TODO
    }
}
