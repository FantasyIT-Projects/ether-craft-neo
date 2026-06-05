package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingUtil;

public class DamagePlatingEffect implements IPlatingEffect {
    @Override
    public double getEffectByEther(long ether) {
        return Math.sqrt(ether) / 10.0;
    }

    @Override
    public boolean onAttack(PlatingData data, ItemStack stack, Player player, Entity target) {
        if (!(target instanceof LivingEntity living)) return false;
        if (!PlatingUtil.canExtractEther(stack, 1)) return false;
        PlatingUtil.extractEther(stack, 1);
        living.hurt(living.damageSources().playerAttack(player), (float) data.effect());
        return true;
    }
}