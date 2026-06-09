package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingCritTrigger {
    void onCriticalHit(PlatingData data, ItemStack stack, LivingEntity entity, CriticalHitEvent event);
}
