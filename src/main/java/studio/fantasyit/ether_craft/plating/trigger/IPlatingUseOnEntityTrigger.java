package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingUseOnEntityTrigger {
    default void onUseOnEntity(PlatingData data, ItemStack stack, LivingEntity entity, Entity target) {
    }
}
