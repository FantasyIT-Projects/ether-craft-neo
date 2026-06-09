package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingCritTrigger;

public class CritPlatingEffect implements IPlatingEffect, IPlatingCritTrigger {
    public static final Identifier ID = EtherCraft.id("crit");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void onCriticalHit(PlatingData data, ItemStack stack, LivingEntity entity, CriticalHitEvent event) {
        if (event.isCriticalHit()) return;
        if (!PlatingUtil.canExtractEther(stack, Config.platingCritEtherPerAttack)) return;

        double chance = data.effect();
        if (entity.getRandom().nextDouble() < chance) {
            event.setCriticalHit(true);
            PlatingUtil.extractEther(stack, Config.platingCritEtherPerAttack);
        }
    }
}
