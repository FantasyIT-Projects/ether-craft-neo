package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

public interface IPlatingArrowShotTrigger extends IPlatingEffect {
    void onArrowShot(PlatingData data, ItemStack stack, Player player, AbstractArrow arrow);
}
