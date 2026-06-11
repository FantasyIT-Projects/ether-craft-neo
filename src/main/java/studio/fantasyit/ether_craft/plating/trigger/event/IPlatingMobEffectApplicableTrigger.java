package studio.fantasyit.ether_craft.plating.trigger.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

public interface IPlatingMobEffectApplicableTrigger {
    void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, MobEffectEvent.Applicable event);
}
