package studio.fantasyit.ether_craft.menu.factory.slot;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.menu.base.slot.BaseSlot;

public class SingleStackSlot extends BaseSlot {
    public SingleStackSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        return 1;
    }
}
