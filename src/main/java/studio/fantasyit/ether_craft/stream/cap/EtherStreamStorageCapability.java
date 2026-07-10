package studio.fantasyit.ether_craft.stream.cap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
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
import org.jetbrains.annotations.UnknownNullability;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.stream.EtherConsumer;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.util.ContainerOps;

import java.util.Collections;
import java.util.List;

public class EtherStreamStorageCapability implements IStreamCapability, Container {

    public static final Codec<EtherStreamStorageCapability> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(c -> c.itemStack)
    ).apply(instance, items -> {
        EtherStreamStorageCapability cap = new EtherStreamStorageCapability(1);
        cap.itemStack = NonNullList.createWithCapacity(items.size());
        cap.itemStack.addAll(items);
        cap.handler = VanillaContainerWrapper.of(cap);
        return cap;
    }));

    private NonNullList<ItemStack> itemStack;
    public ResourceHandler<ItemResource> handler;
    @Nullable
    private EtherConsumer consumer;

    public EtherStreamStorageCapability(int size) {
        this.itemStack = NonNullList.withSize(size, ItemStack.EMPTY);
        handler = VanillaContainerWrapper.of(this);
    }

    public void addSlots(int count) {
        NonNullList<ItemStack> newList = NonNullList.withSize(itemStack.size() + count, ItemStack.EMPTY);
        for (int i = 0; i < itemStack.size(); i++) {
            newList.set(i, itemStack.get(i));
        }
        itemStack = newList;
        handler = VanillaContainerWrapper.of(this);
    }

    public static final Identifier ID = EtherCraft.id("storage");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void getConsumption(EtherConsumer consumer, IEtherStreamLike entity) {
        float consumption = 0;
        float scale = 0.1f;
        for (ItemStack i : itemStack) {
            consumption += 1;
            scale += 0.2f;
            if (!i.isEmpty()) {
                consumption += (float) i.count() / i.getMaxStackSize();
            }
        }
        consumer.addConsumption((int) Math.ceil(consumption * scale));
    }

    @Override
    public void setConsumer(EtherConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void tick(@UnknownNullability IEtherStreamLike streamEntity) {
        if (streamEntity.getCapability(EtherStreamPlatingCapability.ID).isPresent())
            return;
        AABB currentBlockPos = new AABB(streamEntity.blockPosition());
        List<ItemEntity> entities = streamEntity.level().getEntities(EntityTypeTest.forClass(ItemEntity.class), currentBlockPos, t -> t.isAlive() && !t.hasPickUpDelay());
        boolean changed = false;
        if (!entities.isEmpty()) {
            for (ItemEntity e : entities) {
                if (e.hasData(AttachmentDataRegistry.CD_TO_TAKE_BY_ETHER_STREAM))
                    if (e.getData(AttachmentDataRegistry.CD_TO_TAKE_BY_ETHER_STREAM) > e.tickCount)
                        continue;
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
    }

    @Override
    public boolean hitEntity(ServerLevel level, IEtherStreamLike streamEntity, EntityHitResult hit, Entity entity) {
        if (entity instanceof ServerPlayer sp) {
            PlayerInventoryWrapper playerInventoryWrapper = PlayerInventoryWrapper.of(sp);
            ContainerOps.tryPlaceToItemHandler(this, playerInventoryWrapper);
        } else {
            @Nullable ResourceHandler<@NotNull ItemResource> r = entity.getCapability(Capabilities.Item.ENTITY);
            if (r != null)
                ContainerOps.tryPlaceToItemHandler(this, r);
        }
        return false;
    }

    @Override
    public boolean hitBlock(ServerLevel level, IEtherStreamLike streamEntity, BlockHitResult hit, BlockState blockState) {
        ResourceHandler<@NotNull ItemResource> r = level.getCapability(Capabilities.Item.BLOCK, hit.getBlockPos(), hit.getDirection());
        if (r != null)
            ContainerOps.tryPlaceToItemHandler(this, r);
        return false;
    }

    @Override
    public void onDestroy(IEtherStreamLike streamEntity, @Nullable HitResult hitResult) {
        Vec3 _position = streamEntity.position().subtract(streamEntity.deltaMovement());
        Vec3 position = BlockPos.containing(_position).getCenter();
        for (int i = 0; i < getContainerSize(); ++i) {
            dropItemStack(streamEntity.level(), position.x, position.y, position.z, getItem(i));
        }
    }


    public static void dropItemStack(Level level, double x, double y, double z, ItemStack itemStack) {
        double size = EntityType.ITEM.getWidth();
        RandomSource random = level.getRandom();

        while (!itemStack.isEmpty()) {
            ItemEntity entity = new ItemEntity(level, x, y, z, itemStack.split(random.nextInt(21) + 10));
            entity.setData(AttachmentDataRegistry.CD_TO_TAKE_BY_ETHER_STREAM, entity.tickCount + Config.itemPickUpByStreamDelayAfterDropped);
            entity.setDeltaMovement(random.triangle(0.0F, 0.11485000171139836), random.triangle(0.2, 0.11485000171139836), random.triangle(0.0F, 0.11485000171139836));
            level.addFreshEntity(entity);
        }

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
        if (consumer != null) consumer.markDirty();
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
    public void serialize(ValueOutput output) {
        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), itemStack);
    }

    @Override
    public void deserialize(ValueInput input) {
        List<ItemStack> ti = input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        itemStack = NonNullList.createWithCapacity(ti.size());
        itemStack.addAll(ti);
        handler = VanillaContainerWrapper.of(this);
    }
}
