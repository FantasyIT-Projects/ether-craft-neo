package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.helper.PlatingEventHelper;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingRightClickTrigger;

import java.util.List;

public class PlatingTriggerC2SHandler {
    public static void handle(PlatingTriggerC2S packet, Player player) {
        for (ItemStack stack : PlatingEventHelper.getPlatedEquipment(player)) {
            List<PlatingData> data = PlatingUtil.getPlatingData(stack);
            for (PlatingData d : data) {
                if (d.id().equals(packet.effectId())) {
                    IPlatingEffect effect = PlatingEventHelper.getEffect(d.id());
                    if (effect instanceof IPlatingRightClickTrigger trigger) {
                        trigger.onRightClick(d, stack, player);
                        return;
                    }
                }
            }
        }
    }
}
