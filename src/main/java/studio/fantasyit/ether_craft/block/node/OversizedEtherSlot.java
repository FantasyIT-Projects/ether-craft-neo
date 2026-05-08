package studio.fantasyit.ether_craft.block.node;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

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
}
