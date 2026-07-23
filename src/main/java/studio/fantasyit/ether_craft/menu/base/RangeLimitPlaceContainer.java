package studio.fantasyit.ether_craft.menu.base;

import com.mojang.serialization.Codec;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import studio.fantasyit.ether_craft.util.ContainerOps;

public class RangeLimitPlaceContainer implements Container, ValueIOSerializable {
    private final Container container;
    private int accessibleCount;

    public RangeLimitPlaceContainer(Container container, int accessibleCount) {
        this.container = container;
        this.accessibleCount = accessibleCount;
    }

    public void setAccessibleCount(int accessibleCount) {
        this.accessibleCount = accessibleCount;
    }
    public int getAccessibleCount() {
        return accessibleCount;
    }

    @Override
    public int getContainerSize() {
        return container.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return container.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return container.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return container.removeItem(slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return container.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        if(slot < accessibleCount)
            container.setItem(slot, itemStack);
    }

    @Override
    public void setChanged() {
        container.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void clearContent() {
        container.clearContent();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return slot < accessibleCount && container.canPlaceItem(slot, itemStack);
    }

    public ItemStack addItem(ItemStack stack) {
        int remain = stack.getCount();
        int maxStack = stack.getMaxStackSize();
        for (int s = 0; s < getContainerSize() && remain > 0; s++) {
            if (!canPlaceItem(s, stack))
                continue;
            ItemStack exist = getItem(s);
            if (exist.isEmpty())
                continue;
            if (!ItemStack.isSameItemSameComponents(stack, exist))
                continue;
            int space = maxStack - exist.getCount();
            if (space <= 0)
                continue;
            int add = Math.min(remain, space);
            exist.grow(add);
            remain -= add;
        }
        for (int s = 0; s < getContainerSize() && remain > 0; s++) {
            if (!canPlaceItem(s, stack))
                continue;
            if (!getItem(s).isEmpty())
                continue;
            int place = Math.min(remain, maxStack);
            setItem(s, stack.copyWithCount(place));
            remain -= place;
        }
        return remain == 0 ? ItemStack.EMPTY : stack.copyWithCount(remain);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("accessibleCount", Codec.INT, accessibleCount);
        output.store("content", ItemStack.OPTIONAL_CODEC.listOf(), ContainerOps.containerToItemList(container));
    }

    @Override
    public void deserialize(ValueInput input) {
        input.read("content", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(l ->
                ContainerOps.fillContainerByItemList(container, l));
        input.read("accessibleCount", Codec.INT).ifPresent(i -> accessibleCount = i);
    }
}
