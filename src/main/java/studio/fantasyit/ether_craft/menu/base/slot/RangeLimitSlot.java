package studio.fantasyit.ether_craft.menu.base.slot;

import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.menu.base.RangeLimitPlaceContainer;

public class RangeLimitSlot extends BaseSlot {

    private final RangeLimitPlaceContainer handler;

    public RangeLimitSlot(RangeLimitPlaceContainer container, int slot, int x, int y) {
        super(container, slot, x, y);
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
