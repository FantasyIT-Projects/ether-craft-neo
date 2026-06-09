package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingBlockingTrigger {
    void blocked(PlatingData data, LivingEntity entity, ItemStack stack, DamageContainer damage);
}
