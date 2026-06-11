package studio.fantasyit.ether_craft.plating.trigger.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

public interface IPlatingKeyTrigger {
    void onKeyTrigger(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity);
}
