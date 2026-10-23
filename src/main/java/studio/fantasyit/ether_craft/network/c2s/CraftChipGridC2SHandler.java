package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.world.entity.player.Player;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryContainerMenu;

public class CraftChipGridC2SHandler {
    public static void handle(CraftChipGridC2S message, Player player) {
        if (player.containerMenu instanceof EtherProcessFactoryContainerMenu menu) {
            menu.applyCraftGridToChip(message.chipSlot(), message.grid());
        }
    }
}
