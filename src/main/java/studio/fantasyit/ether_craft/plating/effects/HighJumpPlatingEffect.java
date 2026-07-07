package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingKeyTrigger;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingRightClickTrigger;

public class HighJumpPlatingEffect implements IPlatingEffect, IPlatingRightClickTrigger, IPlatingKeyTrigger {
    public static final Identifier ID = EtherCraft.id("high_jump");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, PlayerInteractEvent.RightClickItem event) {
        doHighJump(effect, data, stack, entity);
        event.setCanceled(true);
    }

    @Override
    public void onKeyTrigger(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, EquipmentSlot slot) {
        doHighJump(effect, data, stack, entity);
    }

    private void doHighJump(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (data.isCd(level)) return;

        if (!PlatingUtil.canExtractEther(stack, Config.platingHighJumpEtherCost)) return;
        PlatingUtil.extractEtherWithEntityContext(entity, stack, Config.platingHighJumpEtherCost);

        double height = data.effect() * 1.0;
        entity.setDeltaMovement(entity.getDeltaMovement().x, height, entity.getDeltaMovement().z);
        entity.hurtMarked = true;

        entity.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 60, 0, false, false));

        PlatingData updated = data.copyWithCoolDown(level, Config.platingHighJumpCdTicks);
        PlatingUtil.updatePlatingData(stack, updated);
    }
}
