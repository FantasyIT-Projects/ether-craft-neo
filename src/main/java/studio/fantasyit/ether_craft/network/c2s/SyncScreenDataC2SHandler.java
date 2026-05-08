package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.world.entity.player.Player;
import studio.fantasyit.ether_craft.network.base.ISyncTargetMenu;

public class SyncScreenDataC2SHandler {
    public static void handle(SyncScreenDataC2S message, Player player) {
        if (player.hasContainerOpen() && player.containerMenu instanceof ISyncTargetMenu menu) {
            menu.syncScreenData(message);
        }
    }
}
