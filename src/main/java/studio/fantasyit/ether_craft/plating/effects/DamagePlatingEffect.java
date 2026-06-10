package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingAttackTrigger;

public class DamagePlatingEffect implements IPlatingEffect, IPlatingAttackTrigger {
    public static final Identifier ID = EtherCraft.id("damage");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, AttackEntityEvent event) {
        Entity target = event.getTarget();
        if (!(target instanceof LivingEntity living)) return;
        if (!(entity instanceof Player player)) return;
        if (!PlatingUtil.canExtractEther(stack, 1)) return;
        PlatingUtil.extractEther(stack, 1);
        living.hurt(living.damageSources().playerAttack(player), (float) data.effect());
        event.setCanceled(true);
    }
}
