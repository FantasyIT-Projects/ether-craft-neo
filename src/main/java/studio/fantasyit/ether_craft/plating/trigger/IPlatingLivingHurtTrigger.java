package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

public interface IPlatingLivingHurtTrigger extends IPlatingEffect {
    default void onLivingHurt(PlatingData data, ItemStack stack, Player player, LivingIncomingDamageEvent event) {
    }

    default void onKnockBack(PlatingData data, ItemStack stack, Player player, LivingKnockBackEvent event) {
    }
}
