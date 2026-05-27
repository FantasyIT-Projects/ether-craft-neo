package studio.fantasyit.ether_craft.menu.grid.answer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import studio.fantasyit.ether_craft.util.UIUtil;

import static studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryAsset.ARROW;
import static studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryAsset.MAIN_BG;
import static studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryAsset.SLOT;

public class AnswerFetchScreen extends AbstractContainerScreen<AnswerFetchMenu> {
    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 110;

    private static final int PREV_X = 50;
    private static final int NEXT_X = 112;
    private static final int BTN_Y = 82;
    private static final int PAGE_TEXT_Y = 86;

    public AnswerFetchScreen(AnswerFetchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        UIUtil.nineSliced(graphics, MAIN_BG, getLeftPos(), getTopPos(), imageWidth, imageHeight, 5);

        for (int i = 0; i < AnswerFetchMenu.SLOTS_PER_PAGE; i++) {
            Slot slot = menu.getSlot(i);
            SLOT.blit(graphics, getLeftPos() + slot.x - 1, getTopPos() + slot.y - 1);
        }

        if (menu.currentPage > 0) {
            graphics.blit(ARROW.location,
                    getLeftPos() + PREV_X + ARROW.w, getTopPos() + BTN_Y,
                    getLeftPos() + PREV_X, getTopPos() + BTN_Y + ARROW.h,
                    ARROW.u1, ARROW.u0, ARROW.v0, ARROW.v1);
        }

        if (menu.currentPage < menu.totalPages - 1) {
            ARROW.blit(graphics, getLeftPos() + NEXT_X, getTopPos() + BTN_Y);
        }

        Component pageText = Component.literal((menu.currentPage + 1) + " / " + menu.totalPages);
        graphics.text(font, pageText,
                getLeftPos() + imageWidth / 2 - font.width(pageText) / 2,
                getTopPos() + PAGE_TEXT_Y,
                0x404040);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (menu.currentPage > 0 && isOverPrev(event.x(), event.y())) {
            this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            return true;
        }
        if (menu.currentPage < menu.totalPages - 1 && isOverNext(event.x(), event.y())) {
            this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean isOverPrev(double mouseX, double mouseY) {
        int x = getLeftPos() + PREV_X;
        int y = getTopPos() + BTN_Y;
        return mouseX >= x && mouseX < x + ARROW.w && mouseY >= y && mouseY < y + ARROW.h;
    }

    private boolean isOverNext(double mouseX, double mouseY) {
        int x = getLeftPos() + NEXT_X;
        int y = getTopPos() + BTN_Y;
        return mouseX >= x && mouseX < x + ARROW.w && mouseY >= y && mouseY < y + ARROW.h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
