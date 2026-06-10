package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingRightClickTrigger;

public class DashPlatingEffect implements IPlatingEffect, IPlatingRightClickTrigger {
    public static final Identifier ID = EtherCraft.id("dash");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, PlayerInteractEvent.RightClickItem event) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (data.isCd(level)) return;

        if (!PlatingUtil.canExtractEther(stack, Config.platingDashEtherCost)) return;
        PlatingUtil.extractEther(stack, Config.platingDashEtherCost);

        Vec3 look = entity.getLookAngle();
        double distance = data.effect() * 0.5;
        entity.setDeltaMovement(look.x * distance, 0.1, look.z * distance);
        entity.hurtMarked = true;

        PlatingData updated = data.copyWithCoolDown(level, Config.platingDashCdTicks);
        PlatingUtil.updatePlatingData(stack, updated);

        event.setCanceled(true);
    }
}
