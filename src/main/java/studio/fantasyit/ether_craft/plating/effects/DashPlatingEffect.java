package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingRightClickTrigger;

public class DashPlatingEffect implements IPlatingRightClickTrigger {

    @Override
    public double getEffectByEther(long ether) {
        return Math.sqrt(ether) / 10.0;
    }

    @Override
    public boolean onRightClick(PlatingData data, ItemStack stack, Player player) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        if (data.isCd(level)) return false;

        if (!PlatingUtil.canExtractEther(stack, Config.platingDashEtherCost)) return false;
        PlatingUtil.extractEther(stack, Config.platingDashEtherCost);

        Vec3 look = player.getLookAngle();
        double distance = data.effect() * 0.5;
        player.setDeltaMovement(look.x * distance, 0.1, look.z * distance);
        player.hurtMarked = true;

        PlatingData updated = data.copyWithCoolDown(level, Config.platingDashCdTicks);
        PlatingUtil.updatePlatingData(stack, updated);

        return true;
    }
}
