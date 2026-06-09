package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingKillTrigger {
    void onKill(PlatingData data, ItemStack stack, LivingEntity entity, LivingEntity target, LivingDropsEvent event);
}
