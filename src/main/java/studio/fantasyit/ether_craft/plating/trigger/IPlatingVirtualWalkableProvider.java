package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.plating.data.PlatingData;

public interface IPlatingVirtualWalkableProvider {
    int providerVirtualWalkableAt(PlatingData data, ItemStack stack, Level level, LivingEntity entity, BlockPos pos, @Nullable BlockPos lastGroundPos, Vec3 movement);

    void tickOnBlock(PlatingData data, ItemStack stack, Level level, LivingEntity entity, BlockPos pos);
}
