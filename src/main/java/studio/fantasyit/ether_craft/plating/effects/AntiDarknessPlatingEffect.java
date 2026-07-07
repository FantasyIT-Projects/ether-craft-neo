package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingMobEffectApplicableTrigger;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingTickEquippedTrigger;
import studio.fantasyit.ether_craft.plating.trigger.inst.IEffectStartAndEndTrigger;

public class AntiDarknessPlatingEffect implements IPlatingEffect, IPlatingMobEffectApplicableTrigger, IPlatingTickEquippedTrigger, IEffectStartAndEndTrigger {
    public static final Identifier ID = EtherCraft.id("anti_darkness");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, MobEffectEvent.Applicable event) {
        if (!event.getEffectInstance().is(MobEffects.DARKNESS)
                && !event.getEffectInstance().is(MobEffects.BLINDNESS))
            return;
        if (!PlatingUtil.canExtractEther(stack, Config.platingAntiDarknessEtherPerBlock)) return;
        PlatingUtil.extractEtherWithEntityContext(entity, stack, Config.platingAntiDarknessEtherPerBlock);
        event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, PlayerTickEvent.Post event) {
        if (!(entity instanceof Player player)) return;
        if (!PlatingUtil.canExtractEther(stack, Config.platingAntiDarknessEtherPerTick)) return;
        PlatingUtil.extractEtherWithEntityContext(entity, stack, Config.platingAntiDarknessEtherPerTick);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false));
    }

    @Override
    public void onEffectStarts(LivingEntity entity, PlatingData platingData) {
    }

    @Override
    public void onEffectEnds(LivingEntity entity) {
        if (!(entity instanceof Player player)) return;
        player.removeEffect(MobEffects.NIGHT_VISION);
    }
}
