package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.helper.PlatingEventHelper;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingRightClickTrigger;

import java.util.List;

public class PlatingTriggerC2SHandler {
    public static void handle(PlatingTriggerC2S packet, Player player) {
        for (ItemStack stack : PlatingEventHelper.getPlatedEquipment(player)) {
            List<PlatingData> data = PlatingUtil.getPlatingData(stack);
            for (PlatingData d : data) {
                if (d.id().equals(packet.effectId())) {
                    IPlatingEffect effect = PlatingEventHelper.getEffect(d.id());
                    if (effect instanceof IPlatingRightClickTrigger trigger) {
                        trigger.apply(effect, d, stack, player, new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND));
                        return;
                    }
                }
            }
        }
    }
}
