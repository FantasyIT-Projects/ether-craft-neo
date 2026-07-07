package studio.fantasyit.ether_craft.integration.jei.screen;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.world.inventory.Slot;
import studio.fantasyit.ether_craft.menu.base.slot.FilterSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;

import java.util.ArrayList;
import java.util.List;

public class NodeGhostIngredientReceiver implements IGhostIngredientHandler<EtherAdaptNodeScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(EtherAdaptNodeScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
        ArrayList<Target<I>> l = new ArrayList<>();
        if (ingredient.getItemStack().isEmpty())
            return l;
        for (Slot s : gui.getMenu().slots) {
            if (s instanceof FilterSlot fs && fs.isActive())
                l.add(new FilterSlotTarget<>(gui.getMenu().containerId, s.index, gui.getLeftPos() + s.x, gui.getTopPos() + s.y));
        }
        return l;
    }

    @Override
    public void onComplete() {

    }
}
