package studio.fantasyit.ether_craft.menu.factory.slot;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.menu.base.slot.BaseSlot;

public class FactoryInputSlot extends BaseSlot {
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
