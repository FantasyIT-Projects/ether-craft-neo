package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingAttackTrigger;

public class DamagePlatingEffect implements IPlatingEffect, IPlatingAttackTrigger {
    public static final Identifier ID = EtherCraft.id("damage");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public boolean onAttack(PlatingData data, ItemStack stack, LivingEntity entity, Entity target) {
        if (!(target instanceof LivingEntity living)) return false;
        if (!(entity instanceof Player player)) return false;
        if (!PlatingUtil.canExtractEther(stack, 1)) return false;
        PlatingUtil.extractEther(stack, 1);
        living.hurt(living.damageSources().playerAttack(player), (float) data.effect());
        return true;
    }
}
