package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingEffect {
    double getEffectByEther(long ether);
    default void onHoldTick(PlatingData data, ItemStack stack, Player player) {
    }

    default boolean onBreakBlock(PlatingData data, ItemStack stack, Player player, BlockPos pos, BlockState state) {
        return false;
    }

    default boolean onAttack(PlatingData data, ItemStack stack, Player player, Entity target) {
        return false;
    }

    default void onUse(PlatingData data, ItemStack stack, Player player) {
    }

    default void onUseOnBlock(PlatingData data, ItemStack stack, Player player, BlockPos pos, BlockState state) {
    }

    default void onUseOnEntity(PlatingData data, ItemStack stack, Player player, Entity target) {
    }
}
