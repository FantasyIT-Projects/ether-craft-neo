package studio.fantasyit.ether_craft.menu.factory.slot;

import net.minecraft.world.Container;
import studio.fantasyit.ether_craft.menu.base.slot.BaseSlot;

public class InvisibleSlot extends BaseSlot {
    public InvisibleSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
