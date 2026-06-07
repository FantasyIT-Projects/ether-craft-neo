package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.network.s2c.PlatingSoulStateS2C;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingHoldTickTrigger;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingRightClickTrigger;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

public class SoulProjectionPlatingEffect implements IPlatingRightClickTrigger, IPlatingHoldTickTrigger {

    @Override
    public double getEffectByEther(long ether) {
        return Math.sqrt(ether) / 10.0;
    }

    @Override
    public boolean onRightClick(PlatingData data, ItemStack stack, Player player) {
        if (!(player.level() instanceof ServerLevel level)) return false;

        var attachment = player.getData(AttachmentDataRegistry.PLATING_PLAYER);

        if (attachment.soulActive) {
            attachment.soulActive = false;
            attachment.soulCameraPos = null;
            PacketDistributor.sendToPlayer((ServerPlayer) player, new PlatingSoulStateS2C(false));
        } else {
            if (!PlatingUtil.canExtractEther(stack, Config.platingSoulEtherPerTick)) return false;
            attachment.soulActive = true;
            attachment.soulCameraPos = player.position().add(0, player.getEyeHeight(), 0);
            PacketDistributor.sendToPlayer((ServerPlayer) player, new PlatingSoulStateS2C(true));
        }

        return true;
    }

    @Override
    public void onHoldTick(PlatingData data, ItemStack stack, Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;

        var attachment = player.getData(AttachmentDataRegistry.PLATING_PLAYER);
        if (!attachment.soulActive) return;

        if (!PlatingUtil.canExtractEther(stack, Config.platingSoulEtherPerTick)) {
            attachment.soulActive = false;
            attachment.soulCameraPos = null;
            PacketDistributor.sendToPlayer((ServerPlayer) player, new PlatingSoulStateS2C(false));
            return;
        }
        PlatingUtil.extractEther(stack, Config.platingSoulEtherPerTick);

        if (attachment.soulCameraPos != null) {
            double dist = attachment.soulCameraPos.distanceTo(player.position());
            if (dist > Config.platingSoulMaxRange) {
                attachment.soulActive = false;
                attachment.soulCameraPos = null;
                PacketDistributor.sendToPlayer((ServerPlayer) player, new PlatingSoulStateS2C(false));
            }
        }
    }
}
