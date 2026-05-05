package studio.fantasyit.ether_craft.menu.factory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SingleStackSlot extends Slot{
    public SingleStackSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        return 1;
    }
}
