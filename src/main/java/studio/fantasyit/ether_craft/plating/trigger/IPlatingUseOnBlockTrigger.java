package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingUseOnBlockTrigger {
    @Nullable InteractionResult onUseOnBlock(PlatingData data, ItemStack stack, LivingEntity entity, BlockPos pos, BlockState state);
}
