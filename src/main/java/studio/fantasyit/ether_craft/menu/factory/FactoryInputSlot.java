package studio.fantasyit.ether_craft.menu.factory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FactoryInputSlot extends Slot {
    private final Container internal;
    private final int checkTarget;


    public FactoryInputSlot(Container container, int slot, int x, int y, Container internal, int checkTarget) {
        super(container, slot, x, y);
        this.internal = internal;
        this.checkTarget = checkTarget;
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        if (!internal.getItem(checkTarget).isEmpty()) return false;
        return super.mayPlace(itemStack);
    }
}
