package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

public interface IPlatingAttackTrigger extends IPlatingEffect {
    default boolean onAttack(PlatingData data, ItemStack stack, Player player, Entity target) {
        return false;
    }
}
