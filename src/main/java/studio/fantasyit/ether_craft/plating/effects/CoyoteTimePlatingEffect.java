package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.client.CoyoteTimeAudioPlayer;
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
    public int providerVirtualWalkableAt(PlatingData data, ItemStack stack, Level level, LivingEntity entity, BlockPos pos, @Nullable BlockPos jumpStartAt, Vec3 movement) {
        if (!shouldApplySupport(data, stack, level, entity, pos, jumpStartAt, movement)) {
            if (entity instanceof Player player && player.level().isClientSide())
                CoyoteTimeAudioPlayer.stop(player);
            return Integer.MIN_VALUE;
        }
        if (!data.hasCd()) {
            PlatingUtil.updatePlatingData(stack, data.copyWithCoolDown(level, (long) (data.effect() * 20)));
        }
        if (entity.getY() + movement.y - 1 > jumpStartAt.getY()) {
            if (entity instanceof Player player && player.level().isClientSide())
                CoyoteTimeAudioPlayer.stop(player);
            return jumpStartAt.getY();
        }
        PlatingUtil.extractEtherWithEntityContext(entity, stack, Config.platingCoyoteTimeEtherPerJump);

        if (entity instanceof Player player)
            if (player.level().isClientSide()) {
                CoyoteTimeAudioPlayer.start(player);
            }
        return jumpStartAt.getY();
    }

    private boolean shouldApplySupport(PlatingData data, ItemStack stack, Level level, LivingEntity entity, BlockPos pos, @Nullable BlockPos jumpStartAt, Vec3 movement) {
        if (entity.isShiftKeyDown()) return false;
        if (jumpStartAt == null) return false;
        if (data.effect() <= 0) return false;
        if (data.hasCd() && !data.isCd(level)) return false;
        if (!PlatingUtil.canExtractEther(stack, Config.platingCoyoteTimeEtherPerJump)) return false;
        return true;
    }

    @Override
    public void tickOnBlock(PlatingData data, ItemStack stack, Level level, LivingEntity entity, BlockPos pos) {
        if (data.hasCd()) {
            PlatingUtil.updatePlatingData(stack, data.copyClearCoolDown());
            if (entity instanceof Player player)
                CoyoteTimeAudioPlayer.stop(player);
        }
    }
}
