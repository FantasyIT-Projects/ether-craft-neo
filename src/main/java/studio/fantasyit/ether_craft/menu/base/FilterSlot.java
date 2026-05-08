package studio.fantasyit.ether_craft.menu.base;


import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.node.filter.ItemFilter;

public class FilterSlot extends Slot {
    public boolean readonly;
    public ItemFilter handler;
    public FilterSlot(ItemFilter handler, int index, int x, int y) {
        this(handler, index, x, y,false);
    }
    public FilterSlot(ItemFilter handler, int index, int x, int y, boolean readonly) {
        super(handler, index, x, y);
        this.readonly = readonly;
        this.handler = handler;
    }

    boolean active = true;

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public ItemStack safeInsert(ItemStack p_150657_, int p_150658_) {
        if (readonly) {
            return p_150657_;
        }
        this.set(p_150657_.copy());
        return p_150657_;
    }

    @Override
    public void onTake(Player p_150645_, ItemStack p_150646_) {
        if (readonly) {
            return;
        }
        super.onTake(p_150645_, p_150646_);
        p_150646_.shrink(p_150646_.getCount());
    }

    @Override
    public ItemStack safeTake(int p_150648_, int p_150649_, Player p_150650_) {
        return ItemStack.EMPTY;
    }
}