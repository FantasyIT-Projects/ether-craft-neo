package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

public interface IPlatingBreakBlockTrigger extends IPlatingEffect {
    default boolean onBreakBlock(PlatingData data, ItemStack stack, Player player, BlockPos pos, BlockState state) {
        return false;
    }
}
