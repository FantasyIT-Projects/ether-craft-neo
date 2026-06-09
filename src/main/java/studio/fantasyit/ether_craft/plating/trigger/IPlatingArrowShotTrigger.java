package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingArrowShotTrigger {
    void onArrowShot(PlatingData data, ItemStack stack, LivingEntity entity, AbstractArrow arrow);
}
