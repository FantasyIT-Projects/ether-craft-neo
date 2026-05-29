package studio.fantasyit.ether_craft.item;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.menu.grid.ViewGridScreen;

import java.util.List;

public class ViewGridScreenCreator {
    public static void createAndSetScreen(List<List<ItemStack>> grid){
        Minecraft.getInstance().setScreen(
                new ViewGridScreen(Component.translatable("gui.ether_craft.view_grid"), grid));
    }
}
