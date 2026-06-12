package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.CamouflageState;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingTickEquippedTrigger;
import studio.fantasyit.ether_craft.plating.trigger.inst.IEffectStartAndEndTrigger;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

import java.util.List;

public class CamouflagePlatingEffect implements IPlatingEffect, IPlatingTickEquippedTrigger, IEffectStartAndEndTrigger {
    public static final Identifier ID = EtherCraft.id("camouflage");

    private static final int MOB_CLEAR_INTERVAL = 20;

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, PlayerTickEvent.Post event) {
        if (!(entity instanceof Player player)) return;

        CamouflageState state = player.getExistingData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get())
                .orElse(CamouflageState.INACTIVE);

        Vec3 pos = player.position();
        double dist = pos.distanceTo(state.lastPos());

        if (dist > Config.platingCamouflageSpeedThreshold) {
            if (state.isActive()) {
                deactivate(player);
            }
            player.setData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get(),
                    new CamouflageState(false, 0, BlockPos.ZERO, 0f, pos));
            return;
        }

        int newTicks = state.standStillTicks() + 1;

        if (!state.isActive()) {
            if (newTicks >= Config.platingCamouflageStandDuration) {
                PlatingUtil.extractEther(stack, Config.platingCamouflageEtherCost);
                if (PlatingUtil.getEther(stack) > 0)
                    activate(player, player.blockPosition(), player.getYRot(), pos);
            } else {
                player.setData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get(),
                        new CamouflageState(false, newTicks, BlockPos.ZERO, 0f, pos));
            }
        } else {
            if (newTicks % MOB_CLEAR_INTERVAL == 0) {
                clearMobTargets(player);
            }

            player.setData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get(),
                    new CamouflageState(true, newTicks, state.camouflagePos(), state.camouflageYaw(), player.position()));

            PlatingUtil.addEther(stack, Config.platingCamouflageGainEtherPerTick);
        }
    }

    @Override
    public void onEffectStarts(LivingEntity entity, PlatingData platingData) {
    }

    @Override
    public void onEffectEnds(LivingEntity entity) {
        if (!(entity instanceof Player player)) return;
        CamouflageState state = player.getExistingData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get()).orElse(null);
        if (state != null && state.isActive()) {
            player.setInvisible(false);
        }
        player.setData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get(), CamouflageState.INACTIVE);
    }

    private void activate(Player player, BlockPos pos, float yaw, Vec3 posHash) {
        player.setInvisible(true);
        clearMobTargets(player);
        player.setData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get(),
                new CamouflageState(true, 0, pos, yaw, posHash));
    }

    private void deactivate(Player player) {
        player.setInvisible(false);
        player.setData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get(), CamouflageState.INACTIVE);
    }

    private void clearMobTargets(Player player) {
        List<Mob> mobs = player.level().getEntitiesOfClass(
                Mob.class, player.getBoundingBox().inflate(32));
        for (Mob mob : mobs) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
        }
    }
}
