package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.world.entity.player.Player;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryContainerMenu;

public class FactoryMenuSwitchItemC2SHandler {
    public static void handle(FactoryMenuSwitchItemC2S message, Player player) {
        if (player.hasContainerOpen() && player.containerMenu instanceof EtherProcessFactoryContainerMenu menu) {
            menu.onSwitchItem(message.reverse());
        }
    }
}
