package studio.fantasyit.ether_craft.plating.trigger.inst;

import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.data.PlatingData;

public interface IPlatingClearingTrigger {
    void onClear(PlatingData data, ItemStack stack);
}
