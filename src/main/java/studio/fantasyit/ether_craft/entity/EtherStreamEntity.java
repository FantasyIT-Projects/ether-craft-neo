package studio.fantasyit.ether_craft.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.particle.ether_stream.EtherStreamData;
import studio.fantasyit.ether_craft.register.EntityRegistry;
import studio.fantasyit.ether_craft.register.Tags;
import studio.fantasyit.ether_craft.util.ContainerOps;

import java.util.Collections;
import java.util.List;

public class EtherStreamEntity extends Projectile implements Container {
    static final EntityDataAccessor<Integer> ETHER_COUNT = SynchedEntityData.defineId(EtherStreamEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> ITEM_CONSUMPTION = SynchedEntityData.defineId(EtherStreamEntity.class, EntityDataSerializers.INT);
    private int ether;
    private int itemConsumption;
    NonNullList<ItemStack> itemStack;
    private int lowerConsumeFactor = 0;
    ResourceHandler<ItemResource> handler;

    public static EtherStreamEntity create(Level level, NonNullList<ItemStack> itemStack, int slots, int ether, Vec3 position, Vec3 motion) {
        EtherStreamEntity instance = new EtherStreamEntity(EntityRegistry.ETHER_STREAM_ENTITY.get(), level, slots);
        instance.ether = ether;
        instance.itemConsumption = 0;
        for (int i = 0; i < Math.min(itemStack.size(), slots); i++) {
            ItemStack add = itemStack.get(i);
            instance.itemStack.set(i, add);
        }
        instance.updateItemConsumption();
        instance.setPos(position);
        instance.setDeltaMovement(motion);
        return instance;
    }

    public EtherStreamEntity(EntityType<EtherStreamEntity> etherStreamEntityEntityType, Level level) {
        this(etherStreamEntityEntityType, level, 0);
    }

    public EtherStreamEntity(EntityType<EtherStreamEntity> etherStreamEntityEntityType, Level level, int slots) {
        super(etherStreamEntityEntityType, level);
        this.itemStack = NonNullList.withSize(slots, ItemStack.EMPTY);
        this.ether = this.entityData.get(ETHER_COUNT);
        this.itemConsumption = this.entityData.get(ITEM_CONSUMPTION);
        this.handler = VanillaContainerWrapper.of(this);
    }

    protected float getSize() {
        return (float) (0.002 * Math.log(this.ether) * Math.log(this.ether));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ETHER_COUNT, ether);
        builder.define(ITEM_CONSUMPTION, itemConsumption);
    }

    public void setLowerConsumeFactor(int factor) {
        lowerConsumeFactor = factor;
        updateItemConsumption();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.ether = this.entityData.get(ETHER_COUNT);
            this.itemConsumption = this.entityData.get(ITEM_CONSUMPTION);
        } else {
            this.ether -= this.getConsumption();
            if (ether <= 0) {
                this.discard();
                Containers.dropContents(this.level(), this, this);
            } else {
                this.tryPickUp();
            }
            this.entityData.set(ETHER_COUNT, ether);
        }


        Vec3 vec3 = this.getDeltaMovement();
        HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitresult.getType() != HitResult.Type.MISS)
            this.onHit(hitresult);
        this.setPos(this.getX() + vec3.x, this.getY() + vec3.y, this.getZ() + vec3.z);
        if (!this.level().isClientSide()) {
            if (this.level() instanceof ServerLevel sl)
                sl.sendParticles(new EtherStreamData(0xffffff, this.getSize()), this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.15);
        }
    }

    private void tryPickUp() {
        AABB currentBlockPos = new AABB(blockPosition());
        List<ItemEntity> entities = level().getEntities(EntityTypeTest.forClass(ItemEntity.class), currentBlockPos, t -> t.isAlive() && !t.hasPickUpDelay());

        boolean changed = false;
        if (!entities.isEmpty()) {
            for (ItemEntity e : entities) {
                ItemStack tpItem = e.getItem();
                if (tpItem.isEmpty()) continue;
                int toInsert = tpItem.count();
                try (Transaction transaction = Transaction.openRoot()) {
                    toInsert = handler.insert(ItemResource.of(tpItem), toInsert, transaction);
                }
                if (toInsert != 0) {
                    try (Transaction transaction = Transaction.openRoot()) {
                        handler.insert(ItemResource.of(tpItem), toInsert, transaction);
                        ItemStack copy = tpItem.copy();
                        copy.shrink(toInsert);
                        e.setItem(copy);
                        transaction.commit();
                        changed = true;
                        if (tpItem.isEmpty())
                            e.discard();
                    }
                }
            }
        }
        if (changed) {
            setChanged();
            updateItemConsumption();
        }
    }

    private void updateItemConsumption() {
        int consumption = 0;
        double scale = 0.1;
        for (ItemStack i : itemStack) {
            consumption += 1;
            scale += 0.2;
            if (!i.isEmpty()) {
                consumption += i.count();
            }
        }
        itemConsumption = (int) Math.floor(1.0 * consumption * scale);
    }

    private int getConsumption() {
        return (int) Math.ceil((this.itemConsumption * 0.1 + Math.ceil(0.002 * ether)) * Math.pow(2, lowerConsumeFactor));
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (!this.level().isClientSide())
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hitResult;
                BlockState blockState = this.level().getBlockState(blockHit.getBlockPos());
                if (blockState.is(Tags.ETHER_STREAM_PASS_THROUGH)) {
                    return;
                }
            }
        super.onHit(hitResult);
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult p_37258_) {
        super.onHitBlock(p_37258_);
        if (!this.level().isClientSide()) {
            ResourceHandler<@NotNull ItemResource> r = level().getCapability(Capabilities.Item.BLOCK, p_37258_.getBlockPos(), p_37258_.getDirection());
            if (r != null)
                ContainerOps.tryPlaceToItemHandler(this, r);
            EtherContainer e = level().getCapability(EtherContainer.ETHER_CONTAINER, p_37258_.getBlockPos());
            if (e != null)
                e.receiveEther(this.ether);
        }
        dropAndDiscard();
    }

    @Override
    protected void onHitEntity(EntityHitResult p_37259_) {
        super.onHitEntity(p_37259_);
        if (this.level().isClientSide()) return;
        Entity entity = p_37259_.getEntity();
        if (entity instanceof ServerPlayer sp) {
            PlayerInventoryWrapper playerInventoryWrapper = PlayerInventoryWrapper.of(sp);
            ContainerOps.tryPlaceToItemHandler(this, playerInventoryWrapper);
        } else {
            @Nullable ResourceHandler<@NotNull ItemResource> r = entity.getCapability(Capabilities.Item.ENTITY);
            if (r != null)
                ContainerOps.tryPlaceToItemHandler(this, r);
        }
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
        Containers.dropContents(this.level(), this, this);
        this.discard();
    }

    @Override
    public int getContainerSize() {
        return this.itemStack.size();
    }

    @Override
    public boolean isEmpty() {
        return itemStack.stream().anyMatch(t -> !t.isEmpty());
    }

    @Override
    public @NotNull ItemStack getItem(int p_18941_) {
        if (p_18941_ >= itemStack.size() || p_18941_ < 0)
            return ItemStack.EMPTY;
        return itemStack.get(p_18941_);
    }

    @Override
    public @NotNull ItemStack removeItem(int p_18942_, int p_18943_) {
        return ContainerHelper.removeItem(itemStack, p_18942_, p_18943_);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int p_18951_) {
        return ContainerHelper.takeItem(itemStack, p_18951_);
    }

    @Override
    public void setItem(int p_18944_, @NotNull ItemStack p_18945_) {
        if (p_18944_ >= itemStack.size() || p_18944_ < 0) return;
        itemStack.set(p_18944_, p_18945_);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(@NotNull Player p_18946_) {
        return true;
    }

    @Override
    public void clearContent() {
        Collections.fill(itemStack, ItemStack.EMPTY);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), itemStack);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        List<ItemStack> ti = input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        itemStack = NonNullList.createWithCapacity(ti.size());
        itemStack.addAll(ti);
        handler = VanillaContainerWrapper.of(this);
    }
}
