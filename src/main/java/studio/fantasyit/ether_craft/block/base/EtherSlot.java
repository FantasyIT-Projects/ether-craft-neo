package studio.fantasyit.ether_craft.block.base;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.register.ItemRegistry;

public class EtherSlot extends Slot {
    public EtherSlot(EtherSlotContainer container, int x, int y) {
        super(container, 0, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return itemStack.is(ItemRegistry.ETHER);
    }
}
