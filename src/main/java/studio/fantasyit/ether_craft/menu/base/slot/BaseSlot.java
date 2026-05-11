package studio.fantasyit.ether_craft.menu.base.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class BaseSlot extends Slot {
    private boolean active = true;

    public BaseSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
