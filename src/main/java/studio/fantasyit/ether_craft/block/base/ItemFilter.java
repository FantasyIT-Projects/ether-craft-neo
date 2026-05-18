package studio.fantasyit.ether_craft.block.base;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.util.ContainerOps;

import java.util.Arrays;

public class ItemFilter implements Container, ValueIOSerializable {
    private final Runnable save;
    protected int size;
    public boolean whitelist;
    ItemStack[] items;

    public ItemFilter(int size, Runnable changed) {
        this.save = changed;
        this.size = size;
        this.items = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            items[i] = ItemStack.EMPTY;
        }
    }
    @Override
    public int getContainerSize() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return Arrays.stream(items).allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int p_18941_) {
        return items[p_18941_];
    }

    @Override
    public @NotNull ItemStack removeItem(int p_18942_, int p_18943_) {
        if (!items[p_18942_].isEmpty()) {
            ItemStack ret = items[p_18942_].split(p_18943_);
            this.setChanged();
            return ret;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int p_18951_) {
        if (!items[p_18951_].isEmpty()) {
            ItemStack itemStack = items[p_18951_];
            items[p_18951_] = ItemStack.EMPTY;
            return itemStack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int p_18944_, ItemStack p_18945_) {
        setItemNoTrigger(p_18944_, p_18945_);
        this.setChanged();
    }

    public void setItemNoTrigger(int p_18944_, ItemStack p_18945_) {
        items[p_18944_] = p_18945_.copyWithCount(1);
    }

    @Override
    public void setChanged() {
        save.run();
    }

    @Override
    public boolean stillValid(Player p_18946_) {
        return true;
    }

    @Override
    public void clearContent() {
        Arrays.fill(items, ItemStack.EMPTY);
        this.setChanged();
    }

    @Override
    public boolean canTakeItem(Container p_273520_, int p_272681_, ItemStack p_273702_) {
        return false;
    }



    public boolean accepts(ItemResource stack) {
        return Arrays.stream(items).anyMatch(itemStack -> itemStack.isEmpty() || itemStack.is(stack.getItem()));
    }
    public boolean acceptsAt(ItemResource stack, int index) {
        return items[index].isEmpty() || items[index].is(stack.getItem());
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putBoolean("whitelist",whitelist);
        output.store("filter", ItemStack.OPTIONAL_CODEC.listOf(), ContainerOps.containerToItemList(this));
    }

    @Override
    public void deserialize(ValueInput input) {
        whitelist = input.getBooleanOr("whitelist",false);
        input.read("filter", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(l -> ContainerOps.fillContainerByItemList(this, l));
    }
}