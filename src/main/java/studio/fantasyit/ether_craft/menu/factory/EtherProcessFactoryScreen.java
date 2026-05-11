package studio.fantasyit.ether_craft.menu.factory;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import studio.fantasyit.ether_craft.block.factory.EtherProcessChipManager;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;
import studio.fantasyit.ether_craft.block.factory.FactoryLevelDef;
import studio.fantasyit.ether_craft.menu.base.slot.BaseSlot;
import studio.fantasyit.ether_craft.menu.base.widget.IASwitchButton;
import studio.fantasyit.ether_craft.util.UIUtil;

import java.util.List;

import static studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryAsset.*;

public class EtherProcessFactoryScreen extends AbstractContainerScreen<@NotNull EtherProcessFactoryContainerMenu> {
    EtherProcessFactoryEntity be;
    FactoryLevelDef f;
    Rect2i etherShowArea;
    boolean isFiltering = false;

    public EtherProcessFactoryScreen(EtherProcessFactoryContainerMenu menu, Inventory p_97742_, Component p_97743_) {
        super(menu, p_97742_, p_97743_, menu.entity.getLevelDef().guiSize().x, menu.entity.getLevelDef().guiSize().y);
        be = menu.entity;
        inventoryLabelY = imageHeight - 81;
        f = be.getLevelDef();
    }

    @Override
    protected void init() {
        super.init();

        int rpx = getLeftPos() + f.panelRight().x;
        int rpy = getTopPos() + f.panelRight().y;
        if (f.showPanel()) {
            rpx += 4;
            rpy += 7;
        }
        IASwitchButton iaSwitchButton = addRenderableWidget(new IASwitchButton(
                rpx, rpy,
                BTN_NORMAL, BTN_HOVER,
                BTN_DOWN, BTN_DOWN_HOVER,
                null,
                Component.translatable("menu.ether_craft.factory.filter"),
                Component.translatable("menu.ether_craft.factory.filter"),
                f -> {
                    setUsingFilter(!f);
                    return true;
                }
        ));
        iaSwitchButton.setDown(false);
        setUsingFilter(false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.text(font, Component.literal("Ether:" + be.getEther()), getLeftPos() + 5, getTopPos() + 200, 0xffffffff);
        graphics.text(font, Component.literal("Spd:" + be.pressureBonus), getLeftPos() + 5, getTopPos() + 220, 0xffffffff);
        graphics.text(font, Component.literal("Leak:" + be.leak), getLeftPos() + 5, getTopPos() + 240, 0xffffffff);

        int internalX = f.posInternal().x;
        int internalY = f.posInternal().y;
        for (int i = 0; i < be.ROWS; i++) {
            for (int j = 0; j < be.COLS; j++) {
                ItemStack chipItem = be.internalContainer.getItem(i * be.COLS + j);
                EtherProcessChipManager.ProcessChipRecord r = EtherProcessChipManager.get(chipItem);
                if (r == null) continue;
                int ether = be.currentEther[i][j];
                int color = 0xff26c6da; //RGB#f57f17
                if (ether >= r.maxEther() - r.etherConsume() * 2)
                    color = 0xff81c784; //RGB#81c784
                if (ether < r.etherConsume())
                    color = 0xffe65100;
                if (ether < r.etherRequire())
                    color = 0xffff3d00;

                graphics.fill(
                        getLeftPos() + internalX + j * 18 + 2,
                        getTopPos() + internalY + i * 18 + 2,
                        getLeftPos() + internalX + j * 18 + 5,
                        getTopPos() + internalY + i * 18 + 5,
                        color
                );
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        UIUtil.nineSliced(graphics, MAIN_BG, getLeftPos(), getTopPos(), f.mainSize().x, f.mainSize().y, 5);
        PLAYER_INV.blit(graphics, getLeftPos() + f.posPlayer().x, getTopPos() + f.posPlayer().y);

        for (Slot s : menu.mainUiSlots) {
            if (s instanceof BaseSlot bs && bs.isActive())
                SLOT.blit(graphics, getLeftPos() + s.x - 1, getTopPos() + s.y - 1);
        }
        for (Slot s : menu.filterSlots) {
            if (s instanceof BaseSlot bs && bs.isActive())
                SLOT.blit(graphics, getLeftPos() + s.x - 1, getTopPos() + s.y - 1);
        }

        int internalX = f.posInternal().x;
        int internalY = f.posInternal().y;
        for (int i = 0; i < be.processingInputs.length; i++) {
            int progress = be.processingProgress[i];
            if (progress == 0) continue;
            int progressRealWidth = (int) (1.0 * progress / EtherProcessFactoryEntity.MAX_PROGRESS * 18 * be.COLS);
            for (int j = 0; j < be.COLS; j++) {
                for (int k = 0; k < be.ROWS; k++) {
                    if (be.pathBelongings[k][j] != i) continue;
                    int fillWid = Math.min(Math.max(0, progressRealWidth - j * 18), 18);
                    graphics.fill(
                            getLeftPos() + internalX + j * 18,
                            getTopPos() + internalY + k * 18,
                            getLeftPos() + internalX + j * 18 + fillWid,
                            getTopPos() + internalY + k * 18 + 18,
                            0x80c5e1a5
                    );
                }
            }
        }
        for (int i = 0; i < be.processingRecipes.length; i++) {
            ItemStack it = be.possibleResults.getItem(i);
            if (it.isEmpty()) continue;
            if (be.outputContainer.getItem(i).isEmpty())
                UIUtil.renderItemStackSlotPlaceholder(graphics, it, getLeftPos() + f.posOutput().x + 1, getTopPos() + f.posOutput().y + i * 18 + 1);
        }
        int lpx = getLeftPos() + f.panelLeft().x;
        int lpy = getTopPos() + f.panelLeft().y;
        if (f.showPanel()) {
            UIUtil.nineSliced(graphics, MAIN_BG, lpx, lpy, 26, 36, 5);
            lpx += 4;
            lpy += 7;
        }
        SLOT.blit(graphics, lpx, lpy);
        BAR.blit(graphics, lpx, lpy + 21);
        UIUtil.renderEtherBar(be.getEther() == 0 ? 0 : be.pressureBonus, lpx + 1, lpy + 22, 16, 2, graphics);
        if (mouseX >= lpx && mouseX < lpx + BAR.w && mouseY >= lpy + 21 && mouseY < lpy + 21 + BAR.h)
            graphics.setTooltipForNextFrame(Component.translatable("menu.ether_craft.ether_bar_tooltip", menu.entity.getEther()), mouseX, mouseY);

        int rpx = getLeftPos() + f.panelRight().x;
        int rpy = getTopPos() + f.panelRight().y;
        if (f.showPanel()) {
            UIUtil.nineSliced(graphics, MAIN_BG, rpx, rpy, 26, 36, 5);
            rpx += 4;
            rpy += 7;
        }
        FILTER.blit(graphics, rpx + 4, rpy + 18);

        if (isFiltering) {
            for (int i = 0; i < be.ROWS; i++) {
                ARROW.blit(graphics, getLeftPos() + f.posFilterMark().x, getTopPos() + f.posFilterMark().y + i * 18);
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.menu.getCarried().isEmpty())
            if (hoveredSlot != null && hoveredSlot.hasItem() && menu.internalSlotMapping.containsKey(hoveredSlot.index)) {
                Vector2i v = menu.internalSlotMapping.get(hoveredSlot.index);
                ItemStack item = this.hoveredSlot.getItem();
                List<Component> oTooltip = this.getTooltipFromContainerItem(item);
                oTooltip.add(Component.translatable("tooltip.ether_craft.process_chip_ether", be.currentEther[v.y][v.x]).withStyle(ChatFormatting.BOLD));
                graphics.setTooltipForNextFrame(this.font, oTooltip, item.getTooltipImage(), item, mouseX, mouseY, item.get(DataComponents.TOOLTIP_STYLE));
                return;
            }
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
    }

    protected void setUsingFilter(boolean usingFilter) {
        this.isFiltering = usingFilter;
        menu.filterSlots.forEach(t -> t.setActive(usingFilter));
        menu.internalAndOutputSlots.forEach(t -> t.setActive(!usingFilter));
    }
}
