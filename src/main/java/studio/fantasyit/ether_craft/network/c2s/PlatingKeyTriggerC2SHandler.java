package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;
import studio.fantasyit.ether_craft.plating.helper.PlatingEventHelper;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingKeyTrigger;

import java.util.List;

public class PlatingKeyTriggerC2SHandler {
    public static void handle(PlatingKeyTriggerC2S packet, Player player) {
        for (ItemStack stack : PlatingEventHelper.getPlatedEquipment(player)) {
            List<PlatingData> data = PlatingUtil.getPlatingData(stack);
            for (PlatingData d : data) {
                IPlatingEffect effect = PlatingEventHelper.getEffect(d.id());
                if (effect instanceof IPlatingKeyTrigger trigger) {
                    trigger.onKeyTrigger(effect, d, stack, player);
                }
            }
        }
    }
}
