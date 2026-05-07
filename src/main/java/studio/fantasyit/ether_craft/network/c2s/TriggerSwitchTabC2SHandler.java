package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.world.entity.player.Player;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;

public class TriggerSwitchTabC2SHandler {
    public static void handle(TriggerSwitchTabC2S packet, Player player) {
        if(player.hasContainerOpen() && player.containerMenu instanceof EtherAdaptNodeContainerMenu menu){
            menu.triggerSwitchTabServer(packet.plugin());
        }
    }
}
