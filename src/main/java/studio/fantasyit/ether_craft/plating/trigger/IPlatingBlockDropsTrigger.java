package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingBlockDropsTrigger {
    void onBlockDrops(PlatingData data, ItemStack stack, LivingEntity entity, BlockDropsEvent event);
}
