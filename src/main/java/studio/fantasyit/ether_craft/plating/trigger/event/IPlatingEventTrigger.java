package studio.fantasyit.ether_craft.plating.trigger.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

@FunctionalInterface
public interface IPlatingEventTrigger<T extends Event> {
    void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, T event);
}
