package studio.fantasyit.ether_craft.menu.camouflage;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class CamouflageChestScreen extends AbstractContainerScreen<CamouflageChestMenu> {
    private static final Identifier CHEST_BG = Identifier.withDefaultNamespace(
            "textures/gui/container/generic_54.png");

    public CamouflageChestScreen(CamouflageChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 222);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = 75;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = getLeftPos();
        int y = getTopPos();
        graphics.blit(CHEST_BG, x, y, x + imageWidth, y + imageHeight,
                0f, (float) imageWidth / 256f, 0f, (float) imageHeight / 256f);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, getLeftPos() + titleLabelX, getTopPos() + titleLabelY, 0x404040);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
