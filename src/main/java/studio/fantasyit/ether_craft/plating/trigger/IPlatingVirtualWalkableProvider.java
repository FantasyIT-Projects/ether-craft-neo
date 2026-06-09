package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingVirtualWalkableProvider {
    int providerVirtualWalkableAt(PlatingData data, ItemStack stack, Level level, LivingEntity entity, BlockPos pos, @Nullable BlockPos lastGroundPos);

    void tickOnBlock(PlatingData data, ItemStack stack, Level level, LivingEntity entity, BlockPos pos);
}
