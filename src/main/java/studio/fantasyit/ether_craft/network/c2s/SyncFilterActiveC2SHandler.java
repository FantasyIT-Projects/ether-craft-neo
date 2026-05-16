package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.world.entity.player.Player;
import studio.fantasyit.ether_craft.menu.base.IFilterSwitchable;

public class SyncFilterActiveC2SHandler {
    public static void handle(SyncFilterActiveC2S message, Player player) {
        if (player.hasContainerOpen() && player.containerMenu instanceof IFilterSwitchable s) {
            s.setFilterActive(message.active());
        }
    }
}
