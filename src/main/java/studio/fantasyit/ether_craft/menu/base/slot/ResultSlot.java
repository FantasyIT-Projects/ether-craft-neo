package studio.fantasyit.ether_craft.menu.base.slot;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class ResultSlot extends BaseSlot{
    public ResultSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    public boolean mayPlace(ItemStack itemStack) {
        return false;
    }
}
