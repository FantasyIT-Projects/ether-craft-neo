package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.world.entity.player.Player;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryContainerMenu;

public class CraftChipEditorStateC2SHandler {
    public static void handle(CraftChipEditorStateC2S message, Player player) {
        if (!(player.containerMenu instanceof EtherProcessFactoryContainerMenu menu)) return;
        if (message.open()) menu.beginCraftGridEditing(message.chipSlot());
        else menu.endCraftGridEditing();
    }
}
