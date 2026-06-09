package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingBreakBlockTrigger {
    default boolean onBreakBlock(PlatingData data, ItemStack stack, LivingEntity entity, BlockPos pos, BlockState state) {
        return false;
    }
}
