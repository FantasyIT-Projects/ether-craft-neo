package studio.fantasyit.ether_craft.util;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.menu.base.ImageAsset;

public class UIUtil {
    public static void renderItemStackSlotPlaceholder(GuiGraphicsExtractor graphics, ItemStack itemStack, int x, int y) {
        graphics.item(itemStack, x, y);
        graphics.fill(x, y, x + 16, y + 16, 0x8B8B8B80);
    }

    public static void nineSliced(GuiGraphicsExtractor graphics, ImageAsset asset, int x, int y, int w, int h, int border) {
        // LT - Left Top corner (fixed size, no stretch)
        graphics.blit(asset.location, x, y, x + border, y + border,
                asset.subU(0), asset.subU(border), asset.subV(0), asset.subV(border));

        // T - Top edge (stretched horizontally)
        graphics.blit(asset.location, x + border, y, x + w - border, y + border,
                asset.subU(border), asset.subU(asset.w - border), asset.subV(0), asset.subV(border));

        // RT - Right Top corner (fixed size, no stretch)
        graphics.blit(asset.location, x + w - border, y, x + w, y + border,
                asset.subU(asset.w - border), asset.subU(asset.w), asset.subV(0), asset.subV(border));

        // L - Left edge (stretched vertically) - corrected parameter order
        graphics.blit(asset.location, x, y + border, x + border, y + h - border,
                asset.subU(0), asset.subU(border), asset.subV(border), asset.subV(asset.h - border));

        // C - Center (stretched both directions)
        graphics.blit(asset.location, x + border, y + border, x + w - border, y + h - border,
                asset.subU(border), asset.subU(asset.w - border), asset.subV(border), asset.subV(asset.h - border));

        // R - Right edge (stretched vertically)
        graphics.blit(asset.location, x + w - border, y + border, x + w, y + h - border,
                asset.subU(asset.w - border), asset.subU(asset.w), asset.subV(border), asset.subV(asset.h - border));

        // LB - Left Bottom corner (fixed size, no stretch)
        graphics.blit(asset.location, x, y + h - border, x + border, y + h,
                asset.subU(0), asset.subU(border), asset.subV(asset.h - border), asset.subV(asset.h));

        // B - Bottom edge (stretched horizontally)
        graphics.blit(asset.location, x + border, y + h - border, x + w - border, y + h,
                asset.subU(border), asset.subU(asset.w - border), asset.subV(asset.h - border), asset.subV(asset.h));

        // RB - Right Bottom corner (fixed size, no stretch)
        graphics.blit(asset.location, x + w - border, y + h - border, x + w, y + h,
                asset.subU(asset.w - border), asset.subU(asset.w), asset.subV(asset.h - border), asset.subV(asset.h));
    }

}