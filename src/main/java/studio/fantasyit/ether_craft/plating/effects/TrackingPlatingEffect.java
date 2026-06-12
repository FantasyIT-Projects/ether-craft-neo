package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.data.TrackingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingArrowShotTrigger;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

public class TrackingPlatingEffect implements IPlatingEffect, IPlatingArrowShotTrigger {
    public static final Identifier ID = EtherCraft.id("tracking");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!PlatingUtil.canExtractEther(stack, Config.platingTrackingEtherPerArrow)) return;

        double angle = data.effect();
        if (angle <= 0) return;

        PlatingUtil.extractEther(stack, Config.platingTrackingEtherPerArrow);
        arrow.setData(AttachmentDataRegistry.ARROW_TRACKING.get(),
                new TrackingData(Config.platingTrackingRange, angle));
    }
}
