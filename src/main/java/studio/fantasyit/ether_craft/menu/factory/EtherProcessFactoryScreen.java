package studio.fantasyit.ether_craft.menu.factory;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;
import studio.fantasyit.ether_craft.factory.EtherProcessChipManager;
import studio.fantasyit.ether_craft.factory.FactoryLevelDef;
import studio.fantasyit.ether_craft.menu.base.slot.BaseSlot;
import studio.fantasyit.ether_craft.menu.base.widget.IASwitchButton;
import studio.fantasyit.ether_craft.menu.base.widget.NamePencilButton;
import studio.fantasyit.ether_craft.network.c2s.FactoryMenuSwitchItemC2S;
import studio.fantasyit.ether_craft.network.c2s.SetBlockNameC2S;
import studio.fantasyit.ether_craft.network.c2s.SyncFilterActiveC2S;
import studio.fantasyit.ether_craft.recipe.factory.PathNode;
import studio.fantasyit.ether_craft.register.ItemRegistry;
import studio.fantasyit.ether_craft.util.UIUtil;

import java.util.List;

import static studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryAsset.*;

public class EtherProcessFactoryScreen extends AbstractContainerScreen<@NotNull EtherProcessFactoryContainerMenu> {
    EtherProcessFactoryEntity be;
    FactoryLevelDef f;
    Rect2i etherShowArea;
    boolean isFiltering = false;
    private EditBox nameEdit;
    private NamePencilButton namePencil;
    private boolean wasFocused = false;

    public EtherProcessFactoryScreen(EtherProcessFactoryContainerMenu menu, Inventory p_97742_, Component p_97743_) {
        super(menu, p_97742_, p_97743_, menu.entity.getLevelDef().guiSize().x, menu.entity.getLevelDef().guiSize().y);
        be = menu.entity;
        inventoryLabelY = imageHeight - 81;
        f = be.getLevelDef();
    }

    @Override
    protected void init() {
        super.init();

        int nameX = getLeftPos() + 5;
        int nameY = getTopPos() + 5;
        nameEdit = new EditBox(font, nameX, nameY, 80, 12,
                Component.translatable("ether_craft.gui.name_placeholder")) {
            @Override
            public boolean keyPressed(KeyEvent event) {
                if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                    setFocused(false);
                    return true;
                }
                return super.keyPressed(event);
            }
        };
        nameEdit.setMaxLength(32);
        nameEdit.setBordered(false);
        nameEdit.setValue(be.name);
        addRenderableWidget(nameEdit);

        namePencil = new NamePencilButton(nameX + 80 + 2, nameY + 2, nameEdit,
                PENCIL_ON, PENCIL_OFF);
        addRenderableWidget(namePencil);

        int rpx = getLeftPos() + f.panelRight().x;
        int rpy = getTopPos() + f.panelRight().y;
        if (f.showPanel()) {
            rpx += 5;
            rpy += 6;
        }
        IASwitchButton iaSwitchButton = addRenderableWidget(new IASwitchButton(
                rpx, rpy,
                BTN_NORMAL, BTN_HOVER,
                BTN_DOWN, BTN_DOWN_HOVER,
                null,
                Component.translatable("menu.ether_craft.factory.filter"),
                Component.translatable("menu.ether_craft.factory.filter"),
                f -> {
                    boolean newState = !f;
                    setUsingFilter(newState);
                    ClientPacketDistributor.sendToServer(new SyncFilterActiveC2S(newState));
                    return true;
                }
        ));
        iaSwitchButton.setDown(menu.isFilterActive());
        setUsingFilter(menu.isFilterActive());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (nameEdit != null) {
            boolean focused = nameEdit.isFocused();
            if (wasFocused && !focused) {
                ClientPacketDistributor.sendToServer(new SetBlockNameC2S(
                        be.getBlockPos(), nameEdit.getValue()));
            }
            wasFocused = focused;
            int textWidth = font.width(nameEdit.getValue());
            int textEndX = nameEdit.getX() + textWidth + 2;
            namePencil.setX(textEndX);
            namePencil.setY(nameEdit.getY() + (nameEdit.getHeight() - namePencil.getHeight()) / 2);
        }
        boolean serverState = menu.isFilterActive();
        if (isFiltering != serverState) {
            setUsingFilter(serverState);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean result = super.mouseClicked(event, doubleClick);
        if (nameEdit != null && nameEdit.isFocused()) {
            if (!nameEdit.isMouseOver(event.x(), event.y()) && !namePencil.isMouseOver(event.x(), event.y())) {
                this.setFocused(null);
            }
        }
        return result;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int internalX = f.posInternal().x;
        int internalY = f.posInternal().y;
        if (!menu.isFilterActive()) {
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

        if (menu.quickPlaceChipSlotId >= 0 && menu.getCarried().is(ItemRegistry.WRENCH)) {
            ItemStack itemStack = menu.playerInventory.getItem(menu.quickPlaceChipSlotId);
            graphics.item(itemStack, mouseX - 18, mouseY - 15);
            graphics.setTooltipForNextFrame(font, itemStack, mouseX, mouseY);
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
        if (!menu.isFilterActive()) {
            for (int j = 0; j < be.COLS; j++) {
                for (int k = 0; k < be.ROWS; k++) {
                    int idx = k * be.COLS + j;
                    ItemStack item = menu.entity.possibleIntermediateResults.getItem(idx);
                    if (!item.isEmpty())
                        UIUtil.renderItemStackSlotPlaceholder(
                                graphics,
                                item,
                                getLeftPos() + menu.internalAndOutputSlots.get(idx).x,
                                getTopPos() + menu.internalAndOutputSlots.get(idx).y
                        );
                }
            }
            for (int i = 0; i < be.processingInputs.length; i++) {
                int progress = be.processingProgress[i];
                if (progress == 0) continue;

                for (int j = 0; j < be.COLS; j++) {
                    for (int k = 0; k < be.ROWS; k++) {
                        if (be.pathBelongings[k][j] != i) continue;
                        int currentDepth = be.pathDepth[k][j];
                        int maxDepth = be.pathMaxDepth[i];
                        float localProgress = (((float) progress / (EtherProcessFactoryEntity.MAX_PROGRESS * be.maxMultiplier[i]) - ((float) currentDepth / (maxDepth + 1))) * (maxDepth + 1));

                        fillProgressSlot(graphics, internalX, internalY, k * 18, j * 18,
                                localProgress,
                                PathNode.isDirect(PathNode.Direction.UP, be.pathDirection[k][j]),
                                PathNode.isDirect(PathNode.Direction.DOWN, be.pathDirection[k][j]),
                                PathNode.isDirect(PathNode.Direction.LEFT, be.pathDirection[k][j]),
                                PathNode.isDirect(PathNode.Direction.RIGHT, be.pathDirection[k][j])
                        );
                    }
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
            graphics.setTooltipForNextFrame(List.of(
                    Component.translatable("menu.ether_craft.ether_bar_tooltip", menu.entity.getEther()).getVisualOrderText(),
                    Component.translatable("menu.ether_craft.ether_bar_tooltip_speed", be.pressureBonus).getVisualOrderText(),
                    Component.translatable("menu.ether_craft.ether_bar_tooltip_leak", be.leak).getVisualOrderText()
            ), mouseX, mouseY);

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
        } else {
            for (int i = 0; i < be.ROWS; i++) {
                List<ItemStack> itemList = menu.entity.filters[i].getItemList();
                if (itemList.isEmpty()) continue;
                int idx = Math.toIntExact((Util.getMillis() / 1000) % itemList.size());
                UIUtil.renderItemStackSlotPlaceholder(
                        graphics,
                        itemList.get(idx),
                        getLeftPos() + menu.mainUiSlots.get(i).x,
                        getTopPos() + menu.mainUiSlots.get(i).y
                );
            }
        }
    }

    private void fillProgressSlot(GuiGraphicsExtractor graphics, int internalX, int internalY, int ox, int oy, float localProgress, boolean up, boolean down, boolean left, boolean right) {
        if (localProgress <= 0) return;
        if (localProgress > 1) localProgress = 1;
        int offsetX = getLeftPos() + internalX + ox;
        int offsetY = getTopPos() + internalY + oy;

        /*
         * 九宫格渲染，我们使用十六个点来控制渲染
         *
         * 0-1-2-x
         * | | | |
         * x- - -3
         * | | | |
         * x- - -4
         * | | | |
         * x-x-x-x
         */
        int p1 = 0;
        int p2 = 18;
        int p3 = 0;
        int p4 = 18;
        int vMax = 18;
        int hMax = 18;

        if (up && down)
            vMax = 9;
        if (left && right)
            hMax = 9;

        if (left) {
            p1 += (int) (hMax * localProgress);
        }
        if (right) {
            p2 -= (int) (hMax * localProgress);
        }
        if (up) {
            p3 += (int) (vMax * localProgress);
        }
        if (down) {
            p4 -= (int) (vMax * localProgress);
        }
// 填充上排三个格子（左、中、右）
        graphics.fill(offsetX, offsetY, offsetX + p1, offsetY + p3, 0x80c5e1a5); // 上左
        graphics.fill(offsetX + p1, offsetY, offsetX + p2, offsetY + p3, 0x80c5e1a5); // 上中
        graphics.fill(offsetX + p2, offsetY, offsetX + 18, offsetY + p3, 0x80c5e1a5); // 上右

// 填充中排左右两个格子（跳过中心）
        graphics.fill(offsetX, offsetY + p3, offsetX + p1, offsetY + p4, 0x80c5e1a5); // 中左
        graphics.fill(offsetX + p2, offsetY + p3, offsetX + 18, offsetY + p4, 0x80c5e1a5); // 中右

// 填充下排三个格子（左、中、右）
        graphics.fill(offsetX, offsetY + p4, offsetX + p1, offsetY + 18, 0x80c5e1a5); // 下左
        graphics.fill(offsetX + p1, offsetY + p4, offsetX + p2, offsetY + 18, 0x80c5e1a5); // 下中
        graphics.fill(offsetX + p2, offsetY + p4, offsetX + 18, offsetY + 18, 0x80c5e1a5); // 下右
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

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (menu.getCarried().is(ItemRegistry.WRENCH) && menu.quickPlaceChipSlotId != -1 && this.isQuickCrafting) {
            Slot slot = this.getHoveredSlot();
            if (slot != null && menu.internalSlotMapping.containsKey(slot.index) && !this.quickCraftSlots.contains(slot)) {
                this.quickCraftSlots.add(slot);
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (menu.getCarried().is(ItemRegistry.WRENCH)) {
            ClientPacketDistributor.sendToServer(new FactoryMenuSwitchItemC2S(scrollY > 0));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }
}
