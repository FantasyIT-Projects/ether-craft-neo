package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingLivingHurtTrigger;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingRightClickTrigger;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingTickEquippedTrigger;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

public class BlockPlatingEffect implements IPlatingRightClickTrigger, IPlatingTickEquippedTrigger, IPlatingLivingHurtTrigger {

    @Override
    public double getEffectByEther(long ether) {
        return Math.sqrt(ether) / 10.0;
    }
    // a3*(max(a1,min(a2,e)) - a1) + a4

    @Override
    public boolean onRightClick(PlatingData data, ItemStack stack, Player player) {
        if (!PlatingUtil.canExtractEther(stack, Config.platingBlockEtherPerTick)) return false;
        player.setData(AttachmentDataRegistry.PLATING_BLOCKING.get(), true);
        return false;
    }

    @Override
    public void onHoldTick(PlatingData data, ItemStack stack, Player player) {
        Boolean blocking = player.getData(AttachmentDataRegistry.PLATING_BLOCKING.get());
        if (blocking == null || !blocking) return;

        if (!player.isUsingItem() || player.getUseItem() != stack) {
            player.setData(AttachmentDataRegistry.PLATING_BLOCKING.get(), false);
            return;
        }

        if (!PlatingUtil.canExtractEther(stack, Config.platingBlockEtherPerTick)) {
            player.setData(AttachmentDataRegistry.PLATING_BLOCKING.get(), false);
            return;
        }
        PlatingUtil.extractEther(stack, Config.platingBlockEtherPerTick);
    }

    @Override
    public void onLivingHurt(PlatingData data, ItemStack stack, Player player, LivingIncomingDamageEvent event) {
        Boolean blocking = player.getData(AttachmentDataRegistry.PLATING_BLOCKING.get());
        if (blocking == null || !blocking) return;
        if (!player.isUsingItem() || player.getUseItem() != stack) return;

        if (event.getSource().getEntity() != null) {
            Vec3 attackerPos = event.getSource().getEntity().position();
            Vec3 toPlayer = player.position().subtract(attackerPos).normalize();
            Vec3 lookVec = player.getLookAngle();
            if (toPlayer.dot(lookVec) <= 0) return;
        }

        float newAmount = event.getAmount() * (float) (1.0 - Config.platingBlockDamageReduction);
        event.setAmount(newAmount);
    }

    @Override
    public void onKnockBack(PlatingData data, ItemStack stack, Player player, LivingKnockBackEvent event) {
        Boolean blocking = player.getData(AttachmentDataRegistry.PLATING_BLOCKING.get());
        if (blocking == null || !blocking) return;
        if (!player.isUsingItem() || player.getUseItem() != stack) return;

        event.setStrength(event.getStrength() * (float) Config.platingBlockKnockbackFactor);
    }
}
