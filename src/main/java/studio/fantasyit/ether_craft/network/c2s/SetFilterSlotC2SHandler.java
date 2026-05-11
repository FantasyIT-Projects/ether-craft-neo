package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import studio.fantasyit.ether_craft.menu.base.slot.FilterSlot;

public class SetFilterSlotC2SHandler {
    public static void handle(SetFilterSlotC2S message, Player player) {
        if (player.hasContainerOpen() && player.containerMenu instanceof AbstractContainerMenu menu) {
            if (menu.getSlot(message.slot()) instanceof FilterSlot fs) {
                fs.handler.setItem(message.slot(), message.stack());
            }
        }
    }
}
