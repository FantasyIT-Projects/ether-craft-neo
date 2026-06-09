package studio.fantasyit.ether_craft.plating.trigger.inst;

import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IInstanceTrigger {
    void onPlatted(PlatingData data, ItemStack stack);
}
