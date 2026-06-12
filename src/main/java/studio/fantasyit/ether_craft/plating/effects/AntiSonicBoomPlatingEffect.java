package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingLivingIncomingDamageTrigger;

public class AntiSonicBoomPlatingEffect implements IPlatingEffect, IPlatingLivingIncomingDamageTrigger {
    public static final Identifier ID = EtherCraft.id("anti_sonic_boom");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypes.SONIC_BOOM)) return;
        if (!PlatingUtil.canExtractEther(stack, Config.platingAntiSonicBoomEtherPerBlock)) return;
        PlatingUtil.extractEther(stack, Config.platingAntiSonicBoomEtherPerBlock);
        double reduction = data.effect();
        if (reduction >= 1.0) {
            event.setCanceled(true);
        } else if (reduction > 0) {
            event.setAmount(event.getAmount() * (float) (1.0 - reduction));
        }
    }
}
