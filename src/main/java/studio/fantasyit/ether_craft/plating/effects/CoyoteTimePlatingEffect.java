package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingHoldTickTrigger;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingJumpTrigger;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

public class CoyoteTimePlatingEffect implements IPlatingJumpTrigger, IPlatingHoldTickTrigger {

    private static final long COYOTE_WINDOW = 40L;

    @Override
    public double getEffectByEther(long ether) {
        return Math.sqrt(ether) / 10.0;
    }

    @Override
    public void onHoldTick(PlatingData data, ItemStack stack, Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (player.onGround()) {
            var attachment = player.getData(AttachmentDataRegistry.PLATING_PLAYER);
            attachment.lastOnGroundTick = level.getGameTime();
        }
    }

    @Override
    public boolean canJump(PlatingData data, ItemStack stack, Player player) {
        if (!(player.level() instanceof ServerLevel level)) return false;

        var attachment = player.getData(AttachmentDataRegistry.PLATING_PLAYER);
        long now = level.getGameTime();
        if (now - attachment.lastOnGroundTick > COYOTE_WINDOW) return false;

        if (!PlatingUtil.canExtractEther(stack, Config.platingCoyoteTimeEtherPerJump)) return false;
        PlatingUtil.extractEther(stack, Config.platingCoyoteTimeEtherPerJump);
        return true;
    }
}
