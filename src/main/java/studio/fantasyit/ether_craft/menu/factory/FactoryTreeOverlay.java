package studio.fantasyit.ether_craft.menu.factory;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;
import studio.fantasyit.ether_craft.factory.EtherProcessChipManager;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayoutComposer;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramSpec;
import studio.fantasyit.ether_craft.recipe.factory.render.widget.TreeDiagramWidget;
import studio.fantasyit.ether_craft.util.DraggableWindow;
import studio.fantasyit.ether_craft.util.UIUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryAsset.SLOT;

/**
 * 工厂 GUI 上方的树形可视化浮层。
 *
 * <p>数据来自 {@link FactoryTreeModel}（读取已同步的 path 数据与容器）。含连接芯片的通路会被切分成
 * 多个独立步骤，每个步骤作为一组单独绘制（"识别为多个单独配方"）：下游为父配方，上游为独立子流程，
 * 两组之间通过连接芯片边界节点/上游输出节点展示中间产物。
 */
public class FactoryTreeOverlay {
    private static final int NODE_SIZE = 18;
    /** 单个节点内多颗芯片之间的纵向间距，必须与下面 LayoutConfig 的 chipGap 一致。 */
    private static final int CHIP_GAP = 1;
    private static final int GROUP_GAP = 6;
    private static final int HEADER_HEIGHT = 18;
    private static final int PADDING = 4;
    private static final int NODE_GAP = 8;
    private static final int MIN_SPACING = 26;
    private static final int HEADER_BG = 0x50000000;
    private static final int CONCAT_COLOR = 0xFFFFD54F;
    /** 滚轮 / 加减号能放大到的上限（1.0 = 100%）。 */
    private static final double MAX_ZOOM = 3.0;
    /** 窗口缩放下限；内容高度没有任何下限（取消最小高度限制）。 */
    private static final int MIN_WIN_WIDTH = 72;
    private static final int MIN_WIN_HEIGHT = DraggableWindow.TITLE_HEIGHT + 10;

    private final EtherProcessFactoryEntity be;
    private final TreeDiagramWidget widget;
    /** 通用可拖动窗口：标题栏移动、边框/右下角缩放，几何与窗口装饰都由它负责。 */
    private final DraggableWindow win;
    private final Map<String, NodeVisual> visuals = new HashMap<>();
    private final Map<String, FactoryTreeModel.Group> groupByPrefix = new HashMap<>();
    private final Map<Integer, Integer> stepCountByOutput = new HashMap<>();
    private FactoryTreeModel model = FactoryTreeModel.EMPTY;
    private long lastSignature;
    private boolean built;

    /** 节点渲染数据。物品内容在渲染时现场从容器读取，因此物品变化无需重建布局。 */
    private record NodeVisual(int outputIndex, int stepIndex, int row, int col, int depth, int maxDepth,
                              boolean isInput, boolean isOutput, boolean isBoundary,
                              int attachRow, int attachCol, List<FactoryTreeModel.ChipRef> chips) {
    }

    private record HeaderVisual(int outputIndex, int stepIndex) {
    }

    public FactoryTreeOverlay(EtherProcessFactoryEntity be, int viewX, int viewY, int viewW, int viewH) {
        this.be = be;
        this.win = new DraggableWindow(viewX, viewY, viewW, viewH)
                .setMinSize(MIN_WIN_WIDTH, MIN_WIN_HEIGHT);
        this.widget = new TreeDiagramWidget(contentX(), contentY(), contentW(), contentH());
        // 浮层允许放大查看细节（通用视口默认只能缩到刚好显示整张画布）
        this.widget.getViewport().setMaxZoom(MAX_ZOOM);
    }

    // ------------------------------------------------------------------ 窗口

    private int contentX() {
        return win.contentX();
    }

    private int contentY() {
        return win.contentY();
    }

    private int contentW() {
        return win.contentWidth();
    }

    private int contentH() {
        return win.contentHeight();
    }

    /** 窗口移动/缩放后同步内容区；缩放还需要按新宽度重排（并保留当前缩放与平移）。 */
    private void onWindowGeometryChanged(boolean resized) {
        widget.setBounds(contentX(), contentY(), contentW(), contentH());
        if (!resized) return;
        double panX = widget.getViewport().getPanX();
        double panY = widget.getViewport().getPanY();
        rebuild(true, false);
        // setLayout 会 centerPan，这里恢复缩放前的平移，避免视图乱跳
        widget.getViewport().setPan(panX, panY);
    }

    /** 结构变化时才重建（打开浮层时调用，之后每 tick 调用一次，内部按签名节流）。 */
    public void rebuild() {
        rebuild(false, true);
    }

    /**
     * @param force 忽略结构签名强制重建（窗口尺寸变化时需要按新宽度重排）
     * @param refit 重建后是否把视图重新适配到整张画布
     */
    private void rebuild(boolean force, boolean refit) {
        long signature = FactoryTreeModel.structureSignature(be);
        if (!force && built && signature == lastSignature) return;
        built = true;
        lastSignature = signature;
        model = FactoryTreeModel.build(be);

        visuals.clear();
        groupByPrefix.clear();
        stepCountByOutput.clear();
        widget.clearDecorations();
        widget.clearBindings();

        for (FactoryTreeModel.Group group : model.groups()) {
            stepCountByOutput.merge(group.outputIndex(), 1, Integer::sum);
        }

        List<TreeDiagramLayoutComposer.GroupSpec> specs = new ArrayList<>(model.groups().size());
        for (FactoryTreeModel.Group group : model.groups()) {
            String prefix = prefix(group);
            groupByPrefix.put(prefix, group);
            List<TreeDiagramSpec.NodeSpec> nodeSpecs = new ArrayList<>(group.nodes().size());
            for (FactoryTreeModel.Node node : group.nodes()) {
                int subSlots = Math.max(1, chipsOf(node).size());
                nodeSpecs.add(new TreeDiagramSpec.NodeSpec(node.id(), subSlots, node.nextId(), node.isInput()));
            }
            TreeDiagramSpec spec = new TreeDiagramSpec(
                    nodeSpecs,
                    new TreeDiagramSpec.OutputSpec(0),
                    new TreeDiagramSpec.LayoutConfig(NODE_SIZE, NODE_SIZE, CHIP_GAP, 0,
                            contentW(), 0, PADDING, NODE_GAP, MIN_SPACING));
            specs.add(new TreeDiagramLayoutComposer.GroupSpec(prefix, spec));
        }

        TreeDiagramLayoutComposer.Result result = TreeDiagramLayoutComposer.compose(
                specs, contentW(), GROUP_GAP, HEADER_HEIGHT);

        for (FactoryTreeModel.Group group : model.groups()) {
            String prefix = prefix(group);
            for (FactoryTreeModel.Node node : group.nodes()) {
                visuals.put(prefix + node.id(), new NodeVisual(group.outputIndex(), group.stepIndex(),
                        node.row(), node.col(), node.depth(), group.maxDepth(), node.isInput(),
                        node.id().equals(group.outNodeId()), node.isBoundary(),
                        group.attachRow(), group.attachCol(), chipsOf(node)));
            }
        }

        widget.setLayout(result.layout());
        for (TreeDiagramLayoutComposer.Header header : result.headers()) {
            FactoryTreeModel.Group group = groupByPrefix.get(header.prefix());
            if (group == null) continue;
            widget.addDecoration(header.x(), header.y(), header.width(), header.height(),
                    this::renderHeader, new HeaderVisual(group.outputIndex(), group.stepIndex()));
        }
        for (Map.Entry<String, NodeVisual> entry : visuals.entrySet()) {
            widget.bindNode(entry.getKey(), this::renderNode, this::nodeTooltip, null, entry.getValue());
        }
        if (refit) fitToView(result.layout());
    }

    private static String prefix(FactoryTreeModel.Group group) {
        return "o" + group.outputIndex() + "s" + group.stepIndex() + "_";
    }

    /** 工艺芯片节点的芯片列表；输入/输出/边界节点没有芯片槽。芯片物品按 {@link FactoryTreeModel.ChipRef} 在渲染时现场读取。 */
    private List<FactoryTreeModel.ChipRef> chipsOf(FactoryTreeModel.Node node) {
        if (node.isInput() || node.row() < 0 || node.col() < 0) return List.of();
        return FactoryTreeModel.chipsOf(be, node.row(), node.col());
    }

    /** 初始视图自适应，使整张画布可见。 */
    private void fitToView(TreeDiagramLayout layout) {
        double fit = Math.min(1.0, Math.min(
                (double) contentW() / Math.max(1, layout.canvasWidth),
                (double) contentH() / Math.max(1, layout.canvasHeight)));
        double current = widget.getViewport().getZoom();
        if (current > 0 && fit > 0) {
            widget.getViewport().zoomAt(contentW() / 2.0, contentH() / 2.0, fit / current);
        }
        widget.getViewport().centerPan();
    }

    // ------------------------------------------------------------------ 渲染

    public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        win.renderBackground(graphics);
        if (model.groups().isEmpty()) {
            if (contentH() >= 12) {
                Component text = Component.translatable("menu.ether_craft.factory.tree_view.empty");
                graphics.text(font, text, contentX() + (contentW() - font.width(text)) / 2,
                        contentY() + contentH() / 2 - 4, 0xFF9AA5B9);
            }
        } else {
            widget.render(graphics, font, mouseX, mouseY);
        }
        win.renderChrome(graphics, font, Component.translatable("menu.ether_craft.factory.tree_view"),
                mouseX, mouseY);
    }

    public void renderTooltips(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        if (model.groups().isEmpty()) return;
        widget.renderTooltips(graphics, font, mouseX, mouseY);
    }

    private void renderHeader(GuiGraphicsExtractor graphics, TreeDiagramLayout.PositionedNode node,
                              HeaderVisual visual, double mouseWorldX, double mouseWorldY) {
        int x = node.x();
        int y = node.y();
        int w = node.width();
        int h = node.height();
        graphics.fill(x, y, x + w, y + h, HEADER_BG);
        ItemStack output = outputItem(visual.outputIndex());
        if (!output.isEmpty()) {
            SLOT.blit(graphics, x + 1, y);
            graphics.item(output, x + 2, y + 1);
        }
        int steps = Math.max(1, stepCountByOutput.getOrDefault(visual.outputIndex(), 1));
        graphics.text(Minecraft.getInstance().font,
                Component.translatable("menu.ether_craft.factory.tree_view.step",
                        visual.outputIndex() + 1, visual.stepIndex() + 1, steps),
                x + 22, y + (h - 8) / 2, 0xFFFFFFFF);
    }

    private void renderNode(GuiGraphicsExtractor graphics, TreeDiagramLayout.PositionedNode node,
                            NodeVisual visual, double mouseWorldX, double mouseWorldY) {
        int x = node.x();
        int y = node.y();
        if (visual.isBoundary()) {
            SLOT.blit(graphics, x, y);
            renderBoundaryNode(graphics, x, y, visual);
            return;
        }
        if (visual.isInput()) {
            SLOT.blit(graphics, x, y);
            renderInputNode(graphics, x, y, visual);
            return;
        }
        if (visual.isOutput()) {
            SLOT.blit(graphics, x, y);
            ItemStack output = visual.attachRow() >= 0
                    ? intermediateItem(visual.attachRow(), visual.attachCol())
                    : outputItem(visual.outputIndex());
            if (!output.isEmpty()) graphics.item(output, x + 1, y + 1);
            if (visual.attachRow() >= 0) drawBorder(graphics, x, y, CONCAT_COLOR);
            return;
        }
        // 工艺芯片节点：一个通路格可能同时被上下（或左右）两颗芯片作用，按 chipGap 纵向堆叠
        for (int i = 0; i < visual.chips().size(); i++) {
            int chipY = y + i * (NODE_SIZE + CHIP_GAP);
            SLOT.blit(graphics, x, chipY);
            FactoryTreeModel.ChipRef ref = visual.chips().get(i);
            ItemStack chip = cellItem(ref.row(), ref.col());
            if (chip.isEmpty()) continue;
            graphics.item(chip, x + 1, chipY + 1);
            drawEtherBorder(graphics, x, chipY, ref, chip);
        }
        drawProgress(graphics, x, y, node.height(), visual);
    }

    /** 连接芯片边界：展示上游步骤送入的中间产物（没有预览时退回连接芯片图标）。 */
    private void renderBoundaryNode(GuiGraphicsExtractor graphics, int x, int y, NodeVisual visual) {
        ItemStack upstream = intermediateItem(visual.row(), visual.col());
        ItemStack toDraw = upstream.isEmpty() ? cellItem(visual.row(), visual.col()) : upstream;
        if (!toDraw.isEmpty()) graphics.item(toDraw, x + 1, y + 1);
        drawBorder(graphics, x, y, CONCAT_COLOR);
    }

    private void renderInputNode(GuiGraphicsExtractor graphics, int x, int y, NodeVisual visual) {
        ItemStack stack = be.inputContainer.getItem(visual.row());
        boolean ghost = false;
        if (stack.isEmpty()) {
            List<ItemStack> filtered = be.filters[visual.row()].getItemList();
            if (!filtered.isEmpty()) {
                stack = filtered.get(0);
                ghost = true;
            }
        }
        if (stack.isEmpty()) return;
        if (ghost) {
            UIUtil.renderItemStackSlotPlaceholder(graphics, stack, x + 1, y + 1);
        } else {
            graphics.item(stack, x + 1, y + 1);
        }
    }

    /** 沿用工厂 GUI 里已有的以太配色规则：绿=峰值省电区，橙=高耗区，红=以太不足。 */
    private void drawEtherBorder(GuiGraphicsExtractor graphics, int x, int y,
                                 FactoryTreeModel.ChipRef ref, ItemStack chip) {
        EtherProcessChipManager.ProcessChipRecord record = EtherProcessChipManager.get(chip);
        if (record == null) return;
        int ether = be.currentEther[ref.row()][ref.col()];
        long consume = record.etherConsume();
        int color = 0xFF26C6DA;
        if (ether >= (long) (Config.factoryPeakRatio * consume)) color = 0xFF81C784;
        else if (ether >= consume) color = 0xFFF4A261;
        else color = 0xFFFF3D00;
        drawBorder(graphics, x, y, color);
    }

    private void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x, y, x + NODE_SIZE, y + 1, color);
        graphics.fill(x, y + NODE_SIZE - 1, x + NODE_SIZE, y + NODE_SIZE, color);
        graphics.fill(x, y, x + 1, y + NODE_SIZE, color);
        graphics.fill(x + NODE_SIZE - 1, y, x + NODE_SIZE, y + NODE_SIZE, color);
    }

    /** 与 {@code EtherProcessFactoryScreen} 中的进度公式保持一致。 */
    private void drawProgress(GuiGraphicsExtractor graphics, int x, int y, int height, NodeVisual visual) {
        int progress = be.processingProgress[visual.outputIndex()];
        if (progress <= 0) return;
        int maxMultiplier = Math.max(1, be.maxMultiplier[visual.outputIndex()]);
        int maxDepth = visual.maxDepth();
        double normalized = (double) progress / (EtherProcessFactoryEntity.MAX_PROGRESS * maxMultiplier);
        float local = (float) ((normalized - (double) visual.depth() / (maxDepth + 1)) * (maxDepth + 1));
        if (local <= 0) return;
        if (local > 1) local = 1;
        int width = Math.max(1, (int) (local * (NODE_SIZE - 2)));
        graphics.fill(x + 1, y + height - 3, x + 1 + width, y + height - 1, 0xFFC5E1A5);
    }

    // ------------------------------------------------------------------ 输入

    /** 标题栏拖动窗口；左/右/下边框与右下角缩放；内容区仍然用于平移画布。 */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (win.mouseClicked(mouseX, mouseY, button)) return true;
        return win.inside(mouseX, mouseY) && !model.groups().isEmpty()
                && widget.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        DraggableWindow.DragAction windowAction = win.mouseDragged(mouseX, mouseY, button);
        if (windowAction != DraggableWindow.DragAction.NONE) {
            onWindowGeometryChanged(windowAction == DraggableWindow.DragAction.RESIZE);
            return true;
        }
        return !model.groups().isEmpty() && widget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public void onMouseReleased() {
        win.onMouseReleased();
        widget.mouseReleased(0, 0, 0);
    }

    /** 整个窗口范围内滚轮都能缩放；落在标题栏/边框上时锚点就近折算到内容区。 */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!win.inside(mouseX, mouseY)) return false;
        double anchorX = Mth.clamp(mouseX, contentX(), contentX() + contentW() - 1);
        double anchorY = Mth.clamp(mouseY, contentY(), contentY() + contentH() - 1);
        return widget.mouseScrolled(anchorX, anchorY, scrollX, scrollY);
    }

    public boolean keyPressed(int keyCode) {
        return widget.keyPressed(keyCode);
    }

    // ------------------------------------------------------------------ 工具

    private ItemStack cellItem(int row, int col) {
        if (row < 0 || col < 0) return ItemStack.EMPTY;
        return be.internalContainer.getItem(row * be.COLS + col);
    }

    private ItemStack outputItem(int outputIndex) {
        ItemStack stack = be.possibleResults.getItem(outputIndex);
        if (stack.isEmpty()) stack = be.outputContainer.getItem(outputIndex);
        return stack;
    }

    /** 运行期把上游步骤的产物预览写在连接芯片格上。 */
    private ItemStack intermediateItem(int row, int col) {
        if (row < 0 || col < 0) return ItemStack.EMPTY;
        return be.possibleIntermediateResults.getItem(row * be.COLS + col);
    }

    private List<Component> nodeTooltip(NodeVisual visual, TreeDiagramLayout.PositionedNode node) {
        List<Component> tooltip = new ArrayList<>(3);
        if (visual.isBoundary()) {
            ItemStack upstream = intermediateItem(visual.row(), visual.col());
            tooltip.add(Component.translatable("menu.ether_craft.factory.tree_view.boundary")
                    .withStyle(ChatFormatting.GOLD));
            if (!upstream.isEmpty()) {
                tooltip.add(Component.translatable("menu.ether_craft.factory.tree_view.intermediate",
                        upstream.getHoverName()));
            }
            return tooltip;
        }
        if (visual.isOutput()) {
            ItemStack stack = visual.attachRow() >= 0
                    ? intermediateItem(visual.attachRow(), visual.attachCol())
                    : outputItem(visual.outputIndex());
            if (visual.attachRow() >= 0) {
                tooltip.add(Component.translatable("menu.ether_craft.factory.tree_view.step_output")
                        .withStyle(ChatFormatting.GOLD));
            }
            if (!stack.isEmpty()) tooltip.add(stack.getHoverName());
            return tooltip;
        }
        if (visual.isInput()) {
            ItemStack stack = be.inputContainer.getItem(visual.row());
            if (!stack.isEmpty()) tooltip.add(stack.getHoverName());
            return tooltip;
        }
        // 工艺芯片节点：可能同时挂有多颗芯片，逐个列出名称与以太/倍率
        for (FactoryTreeModel.ChipRef ref : visual.chips()) {
            ItemStack stack = cellItem(ref.row(), ref.col());
            if (stack.isEmpty()) continue;
            tooltip.add(stack.getHoverName());
            EtherProcessChipManager.ProcessChipRecord record = EtherProcessChipManager.get(stack);
            if (record == null) continue;
            int ether = be.currentEther[ref.row()][ref.col()];
            tooltip.add(Component.translatable("tooltip.ether_craft.process_chip_ether", ether)
                    .withStyle(ChatFormatting.BOLD));
            double rawSpeed = record.storage() > 0 ? 1 + (double) ether / record.storage() : 1;
            tooltip.add(Component.translatable("tooltip.ether_craft.process_chip_speed",
                            String.valueOf(Math.max(1, (long) Math.floor(rawSpeed))))
                    .withStyle(ChatFormatting.GRAY));
        }
        return tooltip.isEmpty() ? null : tooltip;
    }
}
