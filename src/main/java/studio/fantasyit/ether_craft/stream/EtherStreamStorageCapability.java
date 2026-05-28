package studio.fantasyit.ether_craft.stream;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.entity.EtherStreamEntity;
import studio.fantasyit.ether_craft.util.ContainerOps;

import java.util.Collections;
import java.util.List;

public class EtherStreamStorageCapability implements IStreamCapability, Container {
    private NonNullList<ItemStack> itemStack;
    public ResourceHandler<ItemResource> handler;

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
    public int getConsumption() {
        double consumption = 0;
        double scale = 0.1;
        for (ItemStack i : itemStack) {
            consumption += 1;
            scale += 0.2;
            if (!i.isEmpty()) {
                consumption += (double) i.count() / i.getMaxStackSize();
            }
        }
        return (int) Math.ceil(consumption * scale);
    }

    @Override
    public void tick(EtherStreamEntity streamEntity) {
        AABB currentBlockPos = new AABB(streamEntity.blockPosition());
        List<ItemEntity> entities = streamEntity.level().getEntities(EntityTypeTest.forClass(ItemEntity.class), currentBlockPos, t -> t.isAlive() && !t.hasPickUpDelay());
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
    }

    @Override
    public boolean hitEntity(ServerLevel level, EtherStreamEntity streamEntity, EntityHitResult hit, Entity entity) {
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
    public boolean hitBlock(ServerLevel level, EtherStreamEntity streamEntity, BlockHitResult hit, BlockState blockState) {
        ResourceHandler<@NotNull ItemResource> r = level.getCapability(Capabilities.Item.BLOCK, hit.getBlockPos(), hit.getDirection());
        if (r != null)
            ContainerOps.tryPlaceToItemHandler(this, r);
        return false;
    }

    @Override
    public void onDestroy(EtherStreamEntity streamEntity) {
        Containers.dropContents(streamEntity.level(), streamEntity, this);
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
