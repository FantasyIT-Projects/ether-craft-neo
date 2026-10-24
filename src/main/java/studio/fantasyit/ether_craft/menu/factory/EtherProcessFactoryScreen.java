package studio.fantasyit.ether_craft.menu.factory;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;
import studio.fantasyit.ether_craft.factory.EtherProcessChipManager;
import studio.fantasyit.ether_craft.factory.FactoryLevelDef;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.menu.base.slot.BaseSlot;
import studio.fantasyit.ether_craft.menu.base.slot.FilterSlot;
import studio.fantasyit.ether_craft.menu.base.widget.IASwitchButton;
import studio.fantasyit.ether_craft.menu.base.widget.NamePencilButton;
import studio.fantasyit.ether_craft.menu.factory.chip.ChipSlotRenderManager;
import studio.fantasyit.ether_craft.menu.factory.window.FactoryWindowLayer;
import studio.fantasyit.ether_craft.menu.factory.window.RecipeTreeWindow;
import studio.fantasyit.ether_craft.network.c2s.CraftChipEditorStateC2S;
import studio.fantasyit.ether_craft.network.c2s.FactoryMenuSwitchItemC2S;
import studio.fantasyit.ether_craft.network.c2s.SetBlockNameC2S;
import studio.fantasyit.ether_craft.network.c2s.SyncFilterActiveC2S;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.recipe.factory.PathNode;
import studio.fantasyit.ether_craft.register.ItemRegistry;
import studio.fantasyit.ether_craft.util.UIUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryAsset.*;

public class EtherProcessFactoryScreen extends AbstractContainerScreen<@NotNull EtherProcessFactoryContainerMenu> {
    EtherProcessFactoryEntity be;
    FactoryLevelDef f;
    Rect2i etherShowArea;
    boolean isFiltering = false;
    private static final int TREE_VIEW_MARGIN = 4;
    /** 配方窗口默认高度与层叠偏移。 */
    private static final int RECIPE_WINDOW_HEIGHT = 120;
    private static final int RECIPE_WINDOW_CASCADE = 12;
    /** 工作台网格浮层：66×66 底板，槽位自 6px 边框 + 1px 起、18px 栅格。 */
    private static final int CRAFT_GRID_SIZE = 66;
    private static final int CRAFT_GRID_SLOT_OFFSET = 7;
    private static final int CRAFT_GRID_GAP = 4;
    private EditBox nameEdit;
    private NamePencilButton namePencil;
    private boolean wasFocused = false;
    private FactoryTreeButton treeButton;
    private IASwitchButton recipeButton;
    private FactoryTreeOverlay treeOverlay;
    /** 由 JEI 配方转移创建的配方窗口；与树形预览窗口共用同一个浮动窗口层。 */
    private final List<RecipeTreeWindow> recipeWindows = new ArrayList<>();
    /** 浮动窗口层：负责配方窗口 / 预览窗口的 z 序、输入派发与渲染。 */
    private final FactoryWindowLayer windowLayer = new FactoryWindowLayer();
    private boolean treeOpen = false;
    private boolean craftGridOpen = false;
    private int craftGridPanelX = 0;
    private int craftGridPanelY = 0;
    /** 浮层打开时刻：用于吞掉"打开这一下"自身的后续点击事件，避免开了立刻被关掉。 */
    private long craftGridOpenedAt = 0;
    /** 被浮层盖住、临时置为不可用的槽位（避免 hover / 点击穿透到下层）。 */
    private final Set<BaseSlot> craftGridOccludedSlots = new HashSet<>();
    /** 当前打开的工厂界面（JEI 配方页会把 Minecraft.screen 换成自己的界面，故另存一份）。 */
    private static @Nullable EtherProcessFactoryScreen current;

    public EtherProcessFactoryScreen(EtherProcessFactoryContainerMenu menu, Inventory p_97742_, Component p_97743_) {
        super(menu, p_97742_, p_97743_, menu.entity.getLevelDef().guiSize().x, menu.entity.getLevelDef().guiSize().y);
        be = menu.entity;
        inventoryLabelY = imageHeight - 81;
        f = be.getLevelDef();
    }

    /** 当前打开的工厂界面；未打开时为 null。 */
    public static @Nullable EtherProcessFactoryScreen current() {
        return current;
    }

    @Override
    protected void init() {
        super.init();
        current = this;

        int nameX = getLeftPos() + f.mainOrigin().x + 5;
        int nameY = getTopPos() + f.mainOrigin().y + 5;
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

        // 右侧工具组：一个 26×65 底板 + 竖排 3 个 16×16 按钮（过滤 / 配方占位 / 树形可视化）
        int rpx = getLeftPos() + f.panelRight().x + FactoryLevelDef.TOOL_BTN_OFFSET_X;
        int rpy = getTopPos() + f.panelRight().y + FactoryLevelDef.TOOL_BTN_OFFSET_Y;
        IASwitchButton iaSwitchButton = addRenderableWidget(new IASwitchButton(
                rpx, rpy,
                BTN_NORMAL, BTN_HOVER,
                BTN_DOWN, BTN_DOWN_HOVER,
                FILTER,
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

        // 过滤器面板正上方的"树形可视化"小面板
        // 配方按钮：弹起 = 没有配方窗口（点击无效），按下 = 存在配方窗口（点击关闭全部）
        recipeButton = addRenderableWidget(new IASwitchButton(
                rpx, rpy + FactoryLevelDef.TOOL_BTN_GAP,
                BTN_NORMAL, BTN_HOVER,
                BTN_DOWN, BTN_DOWN_HOVER,
                RECIPE,
                Component.translatable("menu.ether_craft.factory.recipe.hint"),
                Component.translatable("menu.ether_craft.factory.recipe.close_all"),
                down -> {
                    // 弹起态不可交互；按下态点击 = 关闭所有配方窗口
                    if (!down) return false;
                    closeAllRecipeWindows();
                    return true;
                }));
        recipeButton.setDown(!recipeWindows.isEmpty());

        treeButton = addRenderableWidget(new FactoryTreeButton(
                rpx, rpy + FactoryLevelDef.TOOL_BTN_GAP * 2,
                BTN_NORMAL, BTN_HOVER, BTN_DOWN, BTN_DOWN_HOVER, VIEWER,
                Component.translatable("menu.ether_craft.factory.tree_view"),
                open -> {
                    treeOpen = open;
                    if (treeOverlay == null) return;
                    if (open) {
                        windowLayer.add(treeOverlay);
                        treeOverlay.rebuild();
                    } else {
                        windowLayer.remove(treeOverlay);
                    }
                }));
        treeButton.setDown(treeOpen);

        // 窗口只创建一次：GUI 尺寸变化导致重新 init 时，不丢用户拖动/缩放后的窗口位置
        if (treeOverlay == null) {
            // 默认与工厂 GUI 同宽、左边缘对齐，位于 GUI 正上方；之后可以随意拖动/缩放
            treeOverlay = new FactoryTreeOverlay(be, getLeftPos(), TREE_VIEW_MARGIN,
                    imageWidth,
                    Math.max(0, getTopPos() - TREE_VIEW_MARGIN * 2));
        }
        if (treeOpen) windowLayer.add(treeOverlay);
        else windowLayer.remove(treeOverlay);
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
        if (treeOpen && treeOverlay != null) {
            treeOverlay.rebuild();
        }
        // 配方按钮的按下态完全由"是否存在配方窗口"驱动
        boolean hasRecipeWindows = !recipeWindows.isEmpty();
        if (recipeButton != null && recipeButton.isDown() != hasRecipeWindows) {
            recipeButton.setDown(hasRecipeWindows);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (windowLayer.mouseClicked(event.x(), event.y(), event.button())) {
            blurNameEdit(event.x(), event.y());
            return true;
        }
        if (craftGridOpen) {
            if (!isInCraftGridPanel(event.x(), event.y())) {
                // 同一次点击的下行/重复事件不算"点击面板外"，否则会开了立刻关
                if (Util.getMillis() - craftGridOpenedAt < 250) return true;
                closeCraftGrid();
                return true;
            }
            // 面板内：走原版槽位点击（标记写入本地 craftGrid），再整包上传给服务端
            Slot hit = slotAt(event.x(), event.y());
            boolean handled = super.mouseClicked(event, doubleClick);
            if (hit != null && menu.craftGridSlots.contains(hit)) menu.uploadCraftGrid();
            blurNameEdit(event.x(), event.y());
            return handled;
        } else if (event.button() == 1 && slotAt(event.x(), event.y()) instanceof Slot clickedSlot
                && menu.internalSlotMapping.containsKey(clickedSlot.index)
                && clickedSlot.getItem().is(ItemRegistry.CRAFT_TABLE_CHIP_ITEM)) {
            // 右键工作台芯片：在芯片格左侧展开工作台网格
            openCraftGrid(clickedSlot.index);
            blurNameEdit(event.x(), event.y());
            return true;
        }
        boolean result = super.mouseClicked(event, doubleClick);
        blurNameEdit(event.x(), event.y());
        return result;
    }

    /**
     * 从 JEI 配方转移创建配方窗口。
     *
     * <p>纯客户端行为：不产生任何网络包，配方数据在客户端已同步。
     */
    public void openRecipeWindow(Identifier recipeId, EtherProcessFactoryRecipe recipe) {
        if (recipe == null || recipe.json == null) return;
        int[] pos = nextRecipeWindowPos();
        RecipeTreeWindow window = new RecipeTreeWindow(recipeId, recipe, pos[0], pos[1],
                Math.max(140, imageWidth), RECIPE_WINDOW_HEIGHT);
        recipeWindows.add(window);
        // 新窗口置顶
        windowLayer.add(window);
        if (recipeButton != null) recipeButton.setDown(true);
    }

    /** 关闭所有配方窗口（R 按钮按下态的点击行为）。 */
    private void closeAllRecipeWindows() {
        for (RecipeTreeWindow window : recipeWindows) windowLayer.remove(window);
        recipeWindows.clear();
        if (recipeButton != null) recipeButton.setDown(false);
    }

    /** 依次层叠摆放：以屏幕居中为基准，每个新窗口右下偏移，越界则夹回屏幕内。 */
    private int[] nextRecipeWindowPos() {
        int w = Math.max(140, imageWidth);
        int h = RECIPE_WINDOW_HEIGHT;
        int offset = (recipeWindows.size() % 8) * RECIPE_WINDOW_CASCADE;
        int x = (width - w) / 2 + offset;
        int y = (height - h) / 2 + offset;
        x = Mth.clamp(x, 0, Math.max(0, width - w));
        y = Mth.clamp(y, 0, Math.max(0, height - h));
        return new int[]{x, y};
    }

    /**
     * 点击名称输入框以外的位置时取消其焦点（浮层点击同样适用）。
     */
    private void blurNameEdit(double mouseX, double mouseY) {
        if (nameEdit != null && nameEdit.isFocused()) {
            if (!nameEdit.isMouseOver(mouseX, mouseY) && !namePencil.isMouseOver(mouseX, mouseY)) {
                this.setFocused(null);
            }
        }
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
                    long consume = r.etherConsume();
                    // 指示灯画在背景层之上，浮层打开时要让位，否则会叠在浮层面板上
                    if (craftGridOpen && overlapsCraftGridPanel(internalX + j * 18 + 2, internalY + i * 18 + 2, 3, 3))
                        continue;

                    int color = 0xff26c6da;
                    if (ether >= (long) (Config.factoryPeakRatio * consume))
                        color = 0xff81c784; // 稳态省电区（绿）
                    else if (ether >= consume)
                        color = 0xfff4a261; // 峰值高耗区（橙）
                    else
                        color = 0xffff3d00; // 以太不足（红）

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

        windowLayer.render(graphics, font, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        UIUtil.nineSliced(graphics, MAIN_BG,
                getLeftPos() + f.mainOrigin().x, getTopPos() + f.mainOrigin().y,
                f.mainSize().x, f.mainSize().y, 5);
        PLAYER_INV.blit(graphics, getLeftPos() + f.posPlayer().x, getTopPos() + f.posPlayer().y);

        // 槽位底图：被工作台网格浮层盖住的槽位虽然被置为不可用，但底图仍照常画，
        // 这样浮层边缘露出的部分不会是光秃秃的主板底色。
        for (Slot s : menu.mainUiSlots) {
            if (s instanceof BaseSlot bs && isSlotBaseVisible(bs))
                SLOT.blit(graphics, getLeftPos() + s.x - 1, getTopPos() + s.y - 1);
        }
        for (Slot s : menu.filterSlots) {
            if (s instanceof BaseSlot bs && isSlotBaseVisible(bs))
                SLOT.blit(graphics, getLeftPos() + s.x - 1, getTopPos() + s.y - 1);
        }

        int internalX = f.posInternal().x;
        int internalY = f.posInternal().y;
        if (!menu.isFilterActive()) {
            for (int j = 0; j < be.COLS; j++) {
                for (int k = 0; k < be.ROWS; k++) {
                    int idx = k * be.COLS + j;
                    ItemStack item = menu.entity.possibleIntermediateResults.getItem(idx);
                    // 命中芯片渲染器的格子由渲染器自己画预览，避免与这里的占位预览叠两层
                    if (!item.isEmpty() && !ChipSlotRenderManager.hasRenderer(menu.internalAndOutputSlots.get(idx).getItem()))
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

                        fillProgressSlot(graphics, internalX, internalY, j * 18, k * 18,
                                localProgress,
                                PathNode.isDirect(PathNode.Direction.UP, be.pathDirection[k][j]),
                                PathNode.isDirect(PathNode.Direction.DOWN, be.pathDirection[k][j]),
                                PathNode.isDirect(PathNode.Direction.LEFT, be.pathDirection[k][j]),
                                PathNode.isDirect(PathNode.Direction.RIGHT, be.pathDirection[k][j])
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

            for (int i = 0; i < be.ROWS; i++) {
                for (int j = 0; j < be.COLS; j++) {
                    if (be.pathBelongings[i][j] == -1) continue;
                    if (i > 0 && be.chipColor[i - 1][j] != -1)
                        drawChipEffectTip(graphics, internalX, internalY, i, j, -1, 0, be.chipColor[i - 1][j]);
                    if (j > 0 && be.chipColor[i][j - 1] != -1)
                        drawChipEffectTip(graphics, internalX, internalY, i, j, 0, -1, be.chipColor[i][j - 1]);
                    if (j + 1 < be.COLS && be.chipColor[i][j + 1] != -1)
                        drawChipEffectTip(graphics, internalX, internalY, i, j, 0, 1, be.chipColor[i][j + 1]);
                    if (i + 1 < be.ROWS && be.chipColor[i + 1][j] != -1)
                        drawChipEffectTip(graphics, internalX, internalY, i, j, 1, 0, be.chipColor[i + 1][j]);
                }
            }
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
        long etherTotal = 0;
        for (int i = 0; i < be.ROWS; i++)
            for (int j = 0; j < be.COLS; j++)
                etherTotal += be.currentEther[i][j];
        UIUtil.renderEtherBarProgress(etherTotal, Math.max(1, be.chipEtherMax), lpx + 1, lpy + 22, 16, 2, graphics);
        if (mouseX >= lpx && mouseX < lpx + BAR.w && mouseY >= lpy + 21 && mouseY < lpy + 21 + BAR.h)
            graphics.setTooltipForNextFrame(List.of(
                    Component.translatable("menu.ether_craft.factory.ether_bar_tooltip", etherTotal).getVisualOrderText(),
                    Component.translatable("menu.ether_craft.ether_bar_tooltip_leak", be.leak).getVisualOrderText()
            ), mouseX, mouseY);

        int rpx = getLeftPos() + f.panelRight().x;
        int rpy = getTopPos() + f.panelRight().y;
        if (f.showPanel()) {
            UIUtil.nineSliced(graphics, MAIN_BG, rpx, rpy,
                    FactoryLevelDef.TOOL_PANEL_W, FactoryLevelDef.TOOL_PANEL_H, 5);
        }

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

        // 工作台网格浮层（3×3 标记格子）
        if (craftGridOpen) {
            int gx = getLeftPos() + craftGridPanelX;
            int gy = getTopPos() + craftGridPanelY;
            CRAFTING_GRID.blit(graphics, gx, gy);
            for (Slot slot : menu.craftGridSlots) {
                if (slot.isActive())
                    SLOT.blit(graphics, getLeftPos() + slot.x - 1, getTopPos() + slot.y - 1);
            }
        }
    }

    /**
     * 命中芯片渲染器的内部芯片格：属于 Slot 的原版物品不再绘制（改由渲染器画），
     * 槽位底图仍由 {@link #extractBackground} 负责（底图属于界面，不属于 Slot）。
     */
    @Override
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        if (menu.internalSlotMapping.containsKey(slot.index)
                && ChipSlotRenderManager.render(graphics, menu, slot, slot.getItem(), mouseX, mouseY, getLeftPos(), getTopPos()))
            return;
        super.extractSlot(graphics, slot, mouseX, mouseY);
    }

    private void drawChipEffectTip(GuiGraphicsExtractor graphics, int internalX, int internalY, int i, int j, int di, int dj, int color) {
        int oSlotX = getLeftPos() + internalX + j * 18 + 9;
        int oSlotY = getTopPos() + internalY + i * 18 + 9;
        int sX = dj == 0 ? 1 : 0;
        int sY = di == 0 ? 1 : 0;
        final int dShade = 1;
        final int alphaColor = 0x20000000;
        graphics.fill(
                oSlotX + dj * 8 + sX * (3 + dShade),
                oSlotY + di * 8 + sY * (3 + dShade),
                oSlotX + dj * (8 - dShade) - sX * (4 + dShade),
                oSlotY + di * (8 - dShade) - sY * (4 + dShade),
                alphaColor | color
        );
        graphics.fill(
                oSlotX + dj * 8 + sX * 3,
                oSlotY + di * 8 + sY * 3,
                oSlotX + dj * (8 - 2 * dShade) - sX * 4,
                oSlotY + di * (8 - 2 * dShade) - sY * 4,
                alphaColor | color
        );
        graphics.fill(
                oSlotX + dj * 8 + sX * 3,
                oSlotY + di * 8 + sY * 3,
                oSlotX + dj * (8 - dShade) - sX * 4,
                oSlotY + di * (8 - dShade) - sY * 4,
                alphaColor | color
        );
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
                int ether = be.currentEther[v.y][v.x];
                oTooltip.add(Component.translatable("tooltip.ether_craft.process_chip_ether", ether).withStyle(ChatFormatting.BOLD));
                EtherProcessChipManager.ProcessChipRecord rec = EtherProcessChipManager.get(item);
                if (rec != null) {
                    double rawSpeed = rec.storage() > 0 ? 1 + (double) ether / rec.storage() : 1;
                    String speedTxt = String.valueOf(Math.max(1, (long) Math.floor(rawSpeed)));
                    oTooltip.add(Component.translatable("tooltip.ether_craft.process_chip_speed", speedTxt).withStyle(ChatFormatting.GRAY));
                }
                graphics.setTooltipForNextFrame(this.font, oTooltip, item.getTooltipImage(), item, mouseX, mouseY, item.get(DataComponents.TOOLTIP_STYLE));
                return;
            }
        super.extractTooltip(graphics, mouseX, mouseY);
        windowLayer.renderTooltips(graphics, font, mouseX, mouseY);
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
        if (windowLayer.mouseDragged(event.x(), event.y(), event.button(), dx, dy)) {
            return true;
        }
        if (menu.getCarried().is(ItemRegistry.WRENCH) && menu.quickPlaceChipSlotId != -1 && this.isQuickCrafting) {
            Slot slot = this.getHoveredSlot();
            if (slot != null && menu.internalSlotMapping.containsKey(slot.index)) {
                this.quickCraftSlots.add(slot);
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (windowLayer.mouseScrolled(x, y, scrollX, scrollY)) {
            return true;
        }
        if (menu.getCarried().is(ItemRegistry.WRENCH)) {
            ClientPacketDistributor.sendToServer(new FactoryMenuSwitchItemC2S(scrollY > 0));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        windowLayer.onMouseReleased();
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (craftGridOpen && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            closeCraftGrid();
            return true;
        }
        // 窗口内的视口键盘操作（平移 / 缩放）；ESC 不再关闭子窗口，直接交给原版关闭界面
        if ((nameEdit == null || !nameEdit.isFocused()) && windowLayer.keyPressed(event.key())) {
            return true;
        }
        return super.keyPressed(event);
    }

    /**
     * 按鼠标坐标现算命中的槽位。
     *
     * <p>不能用 {@link #hoveredSlot}：那个字段是"上一帧渲染"时算出来的，
     * 点击事件到达时鼠标可能已经移动（脚本/键鼠混合操作时尤其明显）。
     */
    @Nullable
    private Slot slotAt(double mouseX, double mouseY) {
        // 注意：{@link #isHovering(int, int, int, int, double, double)} 内部会自行加上
        // leftPos/topPos，而 slot.x/slot.y 是 GUI 相对坐标，所以这里直接传屏幕坐标即可。
        for (Slot slot : menu.slots) {
            if (slot.isActive() && this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY))
                return slot;
        }
        return null;
    }

    private boolean isInCraftGridPanel(double mouseX, double mouseY) {
        return mouseX >= getLeftPos() + craftGridPanelX
                && mouseX < getLeftPos() + craftGridPanelX + CRAFT_GRID_SIZE
                && mouseY >= getTopPos() + craftGridPanelY
                && mouseY < getTopPos() + craftGridPanelY + CRAFT_GRID_SIZE;
    }

    /**
     * 打开工作台网格：面板贴在被右键的芯片格左侧（左侧放不下则翻到右侧），
     * 并把 9 个标记格子挪进面板。
     *
     * <p>{@code Slot.x/y} 是 final，无法就地改坐标，因此这里在客户端重建 9 个
     * {@link FilterSlot} 并按原列表索引替换；索引不变，远端槽位对齐不受影响，
     * 服务端完全不参与定位。
     */
    private void openCraftGrid(int chipSlotIndex) {
        Slot clicked = menu.getSlot(chipSlotIndex);
        int px = getLeftPos() + clicked.x - CRAFT_GRID_SIZE - CRAFT_GRID_GAP;
        if (px < getLeftPos() + 2)
            px = getLeftPos() + clicked.x + 18 + CRAFT_GRID_GAP;
        int py = Mth.clamp(getTopPos() + clicked.y + 9 - CRAFT_GRID_SIZE / 2,
                getTopPos() + 2, getTopPos() + imageHeight - CRAFT_GRID_SIZE - 2);
        craftGridPanelX = px - getLeftPos();
        craftGridPanelY = py - getTopPos();

        for (int i = 0; i < menu.craftGridSlots.size(); i++) {
            FilterSlot old = menu.craftGridSlots.get(i);
            FilterSlot replacement = new FilterSlot(menu.craftGrid, i,
                    craftGridPanelX + CRAFT_GRID_SLOT_OFFSET + (i % 3) * 18,
                    craftGridPanelY + CRAFT_GRID_SLOT_OFFSET + (i / 3) * 18);
            replacement.index = old.index;
            replacement.setActive(true);
            menu.slots.set(old.index, replacement);
            menu.craftGridSlots.set(i, replacement);
        }

        menu.editingChipSlot = chipSlotIndex;
        menu.clearCraftGrid();
        craftGridOpen = true;
        craftGridOpenedAt = Util.getMillis();
        applyCraftGridOcclusion();
        // 回填由服务端完成：它把芯片配方解成 3×3 写进 craftGrid，原版槽位同步推到这边
        ClientPacketDistributor.sendToServer(new CraftChipEditorStateC2S(chipSlotIndex, true));
    }

    private void closeCraftGrid() {
        if (!craftGridOpen) return;
        int edited = menu.editingChipSlot;
        craftGridOpen = false;
        menu.editingChipSlot = -1;
        menu.clearCraftGrid();
        for (FilterSlot slot : menu.craftGridSlots) slot.setActive(false);
        restoreCraftGridOcclusion();
        if (edited >= 0)
            ClientPacketDistributor.sendToServer(new CraftChipEditorStateC2S(edited, false));
    }

    /**
     * 浮层是叠在主板上的，原版命中检测只认槽位坐标，所以这里把与浮层矩形相交的槽位
     * 临时置为不可用：{@code getHoveredSlot} 会跳过它们，不会出现下层槽位的高亮 / tooltip / 误点。
     */
    private void applyCraftGridOcclusion() {
        craftGridOccludedSlots.clear();
        for (Slot slot : menu.slots) {
            if (!(slot instanceof BaseSlot baseSlot)) continue;
            if (!baseSlot.isActive() || menu.craftGridSlots.contains(slot)) continue;
            if (!slotOverlapsCraftGridPanel(slot)) continue;
            baseSlot.setActive(false);
            craftGridOccludedSlots.add(baseSlot);
        }
    }

    private boolean slotOverlapsCraftGridPanel(Slot slot) {
        return overlapsCraftGridPanel(slot.x, slot.y, 18, 18);
    }

    /** GUI 相对坐标的矩形是否与工作台网格浮层相交。 */
    private boolean overlapsCraftGridPanel(int guiX, int guiY, int w, int h) {
        return guiX < craftGridPanelX + CRAFT_GRID_SIZE && guiX + w > craftGridPanelX
                && guiY < craftGridPanelY + CRAFT_GRID_SIZE && guiY + h > craftGridPanelY;
    }

    /** 槽位底图是否要画：正常激活的都画，另外被浮层临时遮挡的也要画（只隐藏物品，不隐藏底图）。 */
    private boolean isSlotBaseVisible(BaseSlot slot) {
        return slot.isActive() || craftGridOccludedSlots.contains(slot);
    }

    private void restoreCraftGridOcclusion() {
        for (BaseSlot slot : craftGridOccludedSlots) slot.setActive(true);
        craftGridOccludedSlots.clear();
        // 按当前过滤态重新套一遍（过滤模式下内部/输出槽本来就不该激活）
        setUsingFilter(this.isFiltering);
    }
}
