package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingVirtualWalkableProvider;

public class CoyoteTimePlatingEffect implements IPlatingVirtualWalkableProvider {
    private static final long COYOTE_WINDOW = 40L;

    @Override
    public double getEffectByEther(long ether) {
        return Math.sqrt(ether) / 10.0;
    }

    @Override
    public int providerVirtualWalkableAt(PlatingData data, ItemStack stack, Level level, Player player, BlockPos pos, @Nullable BlockPos jumpStartAt) {
        if (jumpStartAt == null) return Integer.MIN_VALUE;
        if (data.hasCd() && !data.isCd(level)) return Integer.MIN_VALUE;
        if (!PlatingUtil.canExtractEther(stack, Config.platingCoyoteTimeEtherPerJump)) return Integer.MIN_VALUE;
        PlatingUtil.extractEther(stack, Config.platingCoyoteTimeEtherPerJump);
        if (!data.hasCd())
            PlatingUtil.updatePlatingData(stack, data.copyWithCoolDown(level, COYOTE_WINDOW));
        return jumpStartAt.getY();
    }

    @Override
    public void tickOnBlock(PlatingData data, ItemStack stack, Level level, Player player, BlockPos pos) {
        if (data.hasCd())
            PlatingUtil.updatePlatingData(stack, data.copyClearCoolDown());
    }
}
