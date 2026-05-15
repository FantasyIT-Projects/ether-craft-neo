package studio.fantasyit.ether_craft.menu.node.slot;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.menu.base.slot.BaseSlot;

public class OptionalFilterSlot extends BaseSlot {
    ItemFilter filter;
    private boolean enable = true;

    public OptionalFilterSlot(Container container, ItemFilter filter, int slot, int x, int y) {
        super(container, slot, x, y);
        this.filter = filter;
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return (!enable || filter.getItem(this.getContainerSlot()).isEmpty() || ItemStack.isSameItem(filter.getItem(this.getContainerSlot()), itemStack)) && super.mayPlace(itemStack);
    }

    public void setEnableFilter(boolean enable) {
        this.enable = enable;
    }
}
