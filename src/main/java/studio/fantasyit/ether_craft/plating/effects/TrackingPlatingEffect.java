package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.TrackingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingArrowShotTrigger;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

public class TrackingPlatingEffect implements IPlatingEffect, IPlatingArrowShotTrigger {
    public static final Identifier ID = EtherCraft.id("tracking");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void onArrowShot(PlatingData data, ItemStack stack, LivingEntity entity, AbstractArrow arrow) {
        if (!PlatingUtil.canExtractEther(stack, Config.platingTrackingEtherPerArrow)) return;

        PlatingUtil.extractEther(stack, Config.platingTrackingEtherPerArrow);
        arrow.setData(AttachmentDataRegistry.ARROW_TRACKING.get(),
                new TrackingData(Config.platingTrackingRange, Config.platingTrackingStrength));
    }
}
