package studio.fantasyit.ether_craft.menu.factory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.menu.base.BaseSlot;

public class InvisibleSlot extends BaseSlot {
    public InvisibleSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
