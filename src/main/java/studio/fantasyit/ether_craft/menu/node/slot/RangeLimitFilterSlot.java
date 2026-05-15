package studio.fantasyit.ether_craft.menu.node.slot;

import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.menu.base.RangeLimitPlaceContainer;
import studio.fantasyit.ether_craft.menu.base.slot.BaseSlot;

public class RangeLimitFilterSlot extends OptionalFilterSlot {

    private final RangeLimitPlaceContainer handler;

    public RangeLimitFilterSlot(RangeLimitPlaceContainer container, ItemFilter filter, int slot, int x, int y) {
        super(container,filter, slot, x, y);
        this.handler = container;
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return valid() && super.mayPlace(itemStack);
    }

    public boolean valid() {
        return handler.getAccessibleCount() > getContainerSlot();
    }

    @Override
    public boolean isActive() {
        return valid() && super.isActive();
    }
}
