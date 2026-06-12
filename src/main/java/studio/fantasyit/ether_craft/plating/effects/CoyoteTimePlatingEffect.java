package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingVirtualWalkableProvider;

public class CoyoteTimePlatingEffect implements IPlatingEffect, IPlatingVirtualWalkableProvider {
    public static final Identifier ID = EtherCraft.id("coyote_time");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public int providerVirtualWalkableAt(PlatingData data, ItemStack stack, Level level, LivingEntity entity, BlockPos pos, @Nullable BlockPos jumpStartAt) {
        if (entity.isShiftKeyDown()) return Integer.MIN_VALUE;
        if (jumpStartAt == null) return Integer.MIN_VALUE;
        if (data.effect() <= 0) return Integer.MIN_VALUE;
        if (data.hasCd() && !data.isCd(level)) return Integer.MIN_VALUE;
        if (!PlatingUtil.canExtractEther(stack, Config.platingCoyoteTimeEtherPerJump)) return Integer.MIN_VALUE;
        if (!data.hasCd()) {
            PlatingUtil.updatePlatingData(stack, data.copyWithCoolDown(level, (long) (data.effect() * 20)));
        }
        PlatingUtil.extractEther(stack, Config.platingCoyoteTimeEtherPerJump);
        return jumpStartAt.getY();
    }

    @Override
    public void tickOnBlock(PlatingData data, ItemStack stack, Level level, LivingEntity entity, BlockPos pos) {
        if (data.hasCd())
            PlatingUtil.updatePlatingData(stack, data.copyClearCoolDown());
    }
}
