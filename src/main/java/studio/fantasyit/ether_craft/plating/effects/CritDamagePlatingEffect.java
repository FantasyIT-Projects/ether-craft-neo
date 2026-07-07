package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingCritDamageModifier;

public class CritDamagePlatingEffect implements IPlatingEffect, IPlatingCritDamageModifier {
    public static final Identifier ID = EtherCraft.id("crit_damage");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, CriticalHitEvent event) {
        if (!event.isCriticalHit()) return;
        if (data.effect() <= 0) return;
        if (!PlatingUtil.canExtractEther(stack, Config.platingCritDamageEtherPerAttack)) return;

        event.setDamageMultiplier(event.getDamageMultiplier() + (float) data.effect());
        PlatingUtil.extractEtherWithEntityContext(entity, stack, Config.platingCritDamageEtherPerAttack);
    }
}
