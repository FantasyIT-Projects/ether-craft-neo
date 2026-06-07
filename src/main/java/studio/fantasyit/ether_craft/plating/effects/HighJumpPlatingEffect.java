package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingRightClickTrigger;

public class HighJumpPlatingEffect implements IPlatingRightClickTrigger {

    @Override
    public double getEffectByEther(long ether) {
        return Math.sqrt(ether) / 10.0;
    }

    @Override
    public boolean onRightClick(PlatingData data, ItemStack stack, Player player) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        if (data.isCd(level)) return false;

        if (!PlatingUtil.canExtractEther(stack, Config.platingHighJumpEtherCost)) return false;
        PlatingUtil.extractEther(stack, Config.platingHighJumpEtherCost);

        double height = data.effect() * 1.0;
        player.setDeltaMovement(player.getDeltaMovement().x, height, player.getDeltaMovement().z);
        player.hurtMarked = true;

        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 60, 0, false, false));

        PlatingData updated = data.copyWithCoolDown(level, Config.platingHighJumpCdTicks);
        PlatingUtil.updatePlatingData(stack, updated);

        return true;
    }
}
