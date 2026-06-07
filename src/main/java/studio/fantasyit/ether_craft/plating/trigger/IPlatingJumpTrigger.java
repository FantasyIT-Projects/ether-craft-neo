package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

public interface IPlatingJumpTrigger extends IPlatingEffect {
    boolean canJump(PlatingData data, ItemStack stack, Player player);
}
