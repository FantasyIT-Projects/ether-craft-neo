package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingArrowShotTrigger;

public class NoGravityPlatingEffect implements IPlatingEffect, IPlatingArrowShotTrigger {
    public static final Identifier ID = EtherCraft.id("no_gravity");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!PlatingUtil.canExtractEther(stack, Config.platingNoGravityEtherPerArrow)) return;
        PlatingUtil.extractEtherWithEntityContext(entity, stack, Config.platingNoGravityEtherPerArrow);
        arrow.setNoGravity(true);
    }
}
