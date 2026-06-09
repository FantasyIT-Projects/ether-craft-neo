package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingCritDamageModifier;

public class CritDamagePlatingEffect implements IPlatingCritDamageModifier {

    @Override
    public double getEffectByEther(long ether) {
        return Math.sqrt(ether) / 10.0;
    }

    @Override
    public void onCriticalHit(PlatingData data, ItemStack stack, Player player, CriticalHitEvent event) {
        if (!event.isCriticalHit()) return;
        if (data.effect() <= 0) return;
        if (!PlatingUtil.canExtractEther(stack, Config.platingCritDamageEtherPerAttack)) return;

        event.setDamageMultiplier(event.getDamageMultiplier() + (float) data.effect());
        PlatingUtil.extractEther(stack, Config.platingCritDamageEtherPerAttack);
    }
}
