package studio.fantasyit.ether_craft.menu.base.ether;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.register.ItemRegistry;

public class EtherSlot extends Slot {
    public EtherSlot(Container container, int x, int y) {
        super(container, 0, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return itemStack.is(ItemRegistry.ETHER) || itemStack.is(ItemRegistry.ETHER_CREATIVE);
    }
}
