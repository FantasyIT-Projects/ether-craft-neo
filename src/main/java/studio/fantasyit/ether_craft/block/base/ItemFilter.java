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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemFilter implements Container, ValueIOSerializable {
    private final Runnable save;
    protected int size;
    public boolean whitelist;
    ItemStack[] items;
    private boolean allEmpty = true;

    public ItemFilter(int size, Runnable changed) {
        this.save = changed;
        this.size = size;
        this.items = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            items[i] = ItemStack.EMPTY;
        }
    }

    private void updateAllEmpty() {
        for (ItemStack item : items)
            if (!item.isEmpty()) {
                allEmpty = false;
                return;
            }
        allEmpty = true;
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
            updateAllEmpty();
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
            updateAllEmpty();
            return itemStack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int p_18944_, ItemStack p_18945_) {
        setItemNoTrigger(p_18944_, p_18945_);
        updateAllEmpty();
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
        allEmpty = true;
        this.setChanged();
    }

    @Override
    public boolean canTakeItem(Container p_273520_, int p_272681_, ItemStack p_273702_) {
        return false;
    }


    public boolean accepts(ItemStack stack) {
        if (!whitelist && allEmpty)
            return true;
        for (int i = 0; i < size; i++)
            if (acceptsAt(stack, i))
                return true;
        return false;
    }

    public boolean accepts(ItemResource stack) {
        return accepts(stack.toStack());
    }

    public boolean acceptsAt(ItemStack stack, int index) {
        if (items[index].isEmpty())
            return false;
        if (whitelist)
            return ItemStack.isSameItemSameComponents(stack, items[index]);
        else
            return !ItemStack.isSameItemSameComponents(stack, items[index]);
    }

    public boolean acceptsAt(ItemResource stack, int index) {
        return acceptsAt(stack.toStack(), index);
    }

    public boolean acceptsAtAllowEmpty(ItemResource stack, int index) {
        if (items[index].isEmpty())
            return true;
        if (whitelist)
            return ItemStack.isSameItemSameComponents(stack.toStack(), items[index]);
        else
            return !ItemStack.isSameItemSameComponents(stack.toStack(), items[index]);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putBoolean("whitelist", whitelist);
        output.store("filter", ItemStack.OPTIONAL_CODEC.listOf(), ContainerOps.containerToItemList(this));
    }

    @Override
    public void deserialize(ValueInput input) {
        whitelist = input.getBooleanOr("whitelist", false);
        input.read("filter", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(l -> ContainerOps.fillContainerByItemList(this, l));
        updateAllEmpty();
    }

    public List<ItemStack> getItemList() {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack s : items) {
            if (!s.isEmpty())
                list.add(s);
        }
        return list;
    }
}