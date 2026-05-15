package studio.fantasyit.ether_craft.menu.node.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.register.ItemRegistry;

public class OversizedEtherSlot extends Slot {

    public OversizedEtherSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        return this.container.getMaxStackSize(itemStack);
    }

    @Override
    public int getMaxStackSize() {
        return this.container.getMaxStackSize();
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return itemStack.is(ItemRegistry.ETHER) || itemStack.is(ItemRegistry.ETHER_CREATIVE);
    }
}
