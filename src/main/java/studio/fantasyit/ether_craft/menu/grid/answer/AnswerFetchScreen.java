package studio.fantasyit.ether_craft.menu.grid.answer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import studio.fantasyit.ether_craft.recipe.DelayedIngredient;
import studio.fantasyit.ether_craft.recipe.grid.EtherProcessFactoryGrid;
import studio.fantasyit.ether_craft.util.UIUtil;

import java.util.List;

import static studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryAsset.*;

public class AnswerFetchScreen extends AbstractContainerScreen<AnswerFetchMenu> {
    public static final int IMAGE_WIDTH = 140;
    public static final int IMAGE_HEIGHT = 110;

    private static final int PREV_X = 80;
    private static final int NEXT_X = 112;
    private static final int BTN_Y = 82;
    private static final int PAGE_TEXT_Y = 86;

    public EtherProcessFactoryGrid selectedRecipe;

    public AnswerFetchScreen(AnswerFetchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        UIUtil.nineSliced(graphics, MAIN_BG, getLeftPos(), getTopPos(), imageWidth, imageHeight, 5);

        for (int i = 0; i < AnswerFetchMenu.SLOTS_PER_PAGE + 1; i++) {
            Slot slot = menu.getSlot(i);
            SLOT.blit(graphics, getLeftPos() + slot.x - 1, getTopPos() + slot.y - 1);
        }

        if (menu.currentPage > 0) {
            ARROW.blit(graphics, getLeftPos() + PREV_X, getTopPos() + BTN_Y);
        }

        if (menu.currentPage < menu.totalPages - 1) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(getLeftPos() + NEXT_X + (float) ARROW.w / 2, getTopPos() + BTN_Y + (float) ARROW.h / 2);
            graphics.pose().rotate((float) Math.PI);
            graphics.fill(0, 0, 1, 1, 0xFFFF00FF);
            ARROW.blit(graphics, -ARROW.w / 2, -ARROW.h / 2);
            graphics.pose().popMatrix();
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
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            ItemStack item = hoveredSlot.getItem();
            List<Component> tooltipFromContainerItem = this.getTooltipFromContainerItem(item);
            if (hoveredSlot.index != menu.resultSlot.index)
                tooltipFromContainerItem.add(Component.translatable("ether_craft.gui.answer.select_for_next").withStyle(ChatFormatting.DARK_GRAY));
            else {
                EtherProcessFactoryGrid grid = menu.selectedGrid;
                if (grid != null) {
                    tooltipFromContainerItem.add(Component.translatable("ether_craft.gui.answer.inputs").withStyle(ChatFormatting.GREEN));
                    for (DelayedIngredient ipt : grid.getInputs()) {
                        tooltipFromContainerItem.add(ipt.toIngredient().ingredient().display().resolveForFirstStack(SlotDisplayContext.fromLevel(menu.player.level())).getHoverName());
                    }
                    tooltipFromContainerItem.add(Component.translatable("ether_craft.gui.answer.outputs").withStyle(ChatFormatting.AQUA));
                    tooltipFromContainerItem.add(grid.getTarget().getHoverName());
                } else {
                    tooltipFromContainerItem.add(Component.translatable("ether_craft.gui.answer.no_recipe").withStyle(ChatFormatting.RED));
                }
            }
            graphics.setTooltipForNextFrame(this.font, tooltipFromContainerItem, item.getTooltipImage(), item, mouseX, mouseY, item.get(DataComponents.TOOLTIP_STYLE));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
