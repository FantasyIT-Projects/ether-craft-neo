package studio.fantasyit.ether_craft.util;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public class UIUtil {
    public static void renderItemStackSlotPlaceholder(GuiGraphicsExtractor graphics, ItemStack itemStack, int x, int y) {
        graphics.item(itemStack, x, y);
        graphics.fill(x, y, x + 16, y + 16, 0x8B8B8B80);
    }
}