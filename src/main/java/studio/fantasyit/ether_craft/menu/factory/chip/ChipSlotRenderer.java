package studio.fantasyit.ether_craft.menu.factory.chip;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryContainerMenu;

public interface ChipSlotRenderer {
    boolean predicate(ItemStack itemStack);

    void render(GuiGraphicsExtractor graphics, EtherProcessFactoryContainerMenu menu,
                int mouseX, int mouseY, int slotX, int slotY, int screenX, int screenY, ItemStack stack);
}
