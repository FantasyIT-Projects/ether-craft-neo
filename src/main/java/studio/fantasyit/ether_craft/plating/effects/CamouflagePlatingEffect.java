package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.plating.CamouflageState;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingTickEquippedTrigger;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

import java.util.List;

public class CamouflagePlatingEffect implements IPlatingEffect, IPlatingTickEquippedTrigger {

    private static final int MOB_CLEAR_INTERVAL = 20;

    @Override
    public double getEffectByEther(long ether) {
        return 1.0;
    }

    @Override
    public void onHoldTick(PlatingData data, ItemStack stack, Player player) {
        CamouflageState state = player.getExistingData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get())
                .orElse(CamouflageState.INACTIVE);

        Vec3 pos = player.position();
        long posHash = hashPosition(pos);

        if (posHash != state.lastPosHash()) {
            if (state.isActive()) {
                deactivate(player);
            }
            player.setData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get(),
                    new CamouflageState(false, 0, BlockPos.ZERO, 0f, posHash));
            return;
        }

        int newTicks = state.standStillTicks() + 1;

        if (!state.isActive()) {
            if (newTicks >= Config.platingCamouflageStandDuration) {
                activate(player, player.blockPosition(), player.getYRot(), posHash);
            } else {
                player.setData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get(),
                        new CamouflageState(false, newTicks, BlockPos.ZERO, 0f, posHash));
            }
        } else {
            if (!PlatingUtil.canExtractEther(stack, Config.platingCamouflageEtherPerTick)) {
                deactivate(player);
                return;
            }
            PlatingUtil.extractEther(stack, Config.platingCamouflageEtherPerTick);

            if (newTicks % MOB_CLEAR_INTERVAL == 0) {
                clearMobTargets(player);
            }

            player.setData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get(),
                    new CamouflageState(true, newTicks, state.camouflagePos(), state.camouflageYaw(), posHash));
        }
    }

    private void activate(Player player, BlockPos pos, float yaw, long posHash) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, -1, 0, false, false));
        clearMobTargets(player);
        player.setData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get(),
                new CamouflageState(true, 0, pos, yaw, posHash));
    }

    private void deactivate(Player player) {
        player.removeEffect(MobEffects.INVISIBILITY);
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

    private static long hashPosition(Vec3 pos) {
        long x = (long) (pos.x * 1000.0);
        long y = (long) (pos.y * 1000.0);
        long z = (long) (pos.z * 1000.0);
        return x ^ (y << 11) ^ (z << 22);
    }
}
