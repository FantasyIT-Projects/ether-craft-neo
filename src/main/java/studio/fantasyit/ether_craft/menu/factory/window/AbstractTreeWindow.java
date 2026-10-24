package studio.fantasyit.ether_craft.menu.factory.window;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;
import studio.fantasyit.ether_craft.recipe.factory.render.widget.TreeDiagramWidget;
import studio.fantasyit.ether_craft.util.DraggableWindow;

/**
 * 树形图浮动窗口的共同基础设施：窗口几何（拖动 / 缩放）、画布视口（平移 / 缩放）、
 * 尺寸变化后重建布局并保持缩放与平移、初始自适应。
 *
 * <p>子类只负责"数据 → 布局 + 节点绑定"（{@link #rebuildLayout(boolean, boolean)}）与
 * 标题 / 空态文案，不再各写一套窗口与输入处理。
 */
public abstract class AbstractTreeWindow implements FactoryFloatingWindow {
    /** 单个芯片槽尺寸，与 {@code TreeDiagramSpec} 的 slotSize 一致。 */
    protected static final int NODE_SIZE = 18;
    /** 同一节点内多颗芯片之间的纵向间距。 */
    protected static final int CHIP_GAP = 1;
    protected static final int GROUP_GAP = 6;
    protected static final int HEADER_HEIGHT = 18;
    protected static final int PADDING = 4;
    protected static final int NODE_GAP = 8;
    protected static final int MIN_SPACING = 26;
    /** 滚轮 / 加减号能放大到的上限（1.0 = 100%）。 */
    private static final double MAX_ZOOM = 3.0;
    /** 窗口缩放下限；内容高度没有任何下限（取消最小高度限制）。 */
    private static final int MIN_WIN_WIDTH = 72;
    private static final int MIN_WIN_HEIGHT = DraggableWindow.TITLE_HEIGHT + 10;
    private static final int EMPTY_TEXT_COLOR = 0xFF9AA5B9;

    /** 通用可拖动窗口：标题栏移动、边框 / 右下角缩放，几何与窗口装饰都由它负责。 */
    protected final DraggableWindow win;
    protected final TreeDiagramWidget widget;

    protected AbstractTreeWindow(int x, int y, int width, int height) {
        this.win = new DraggableWindow(x, y, width, height).setMinSize(MIN_WIN_WIDTH, MIN_WIN_HEIGHT);
        this.widget = new TreeDiagramWidget(win.contentX(), win.contentY(), win.contentWidth(), win.contentHeight());
        this.widget.getViewport().setMaxZoom(MAX_ZOOM);
    }

    // ------------------------------------------------------------------ 子类钩子

    /** 标题栏文案。 */
    protected abstract Component title();

    /** 内容是否为空（为空时绘制提示文案，且不接管画布内的点击）。 */
    protected abstract boolean isEmptyModel();

    /** 空态文案；返回 null 表示不绘制。 */
    protected abstract @Nullable Component emptyText();

    /**
     * 重建布局并重新绑定节点。
     *
     * @param force 忽略结构签名强制重建（窗口尺寸变化时需要按新宽度重排）
     * @param refit 重建后是否把视图重新适配到整张画布
     */
    protected abstract void rebuildLayout(boolean force, boolean refit);

    // ------------------------------------------------------------------ 几何

    protected int contentX() {
        return win.contentX();
    }

    protected int contentY() {
        return win.contentY();
    }

    protected int contentW() {
        return win.contentWidth();
    }

    protected int contentH() {
        return win.contentHeight();
    }

    /** 窗口移动 / 缩放后同步内容区；缩放还需要按新宽度重排（并保留当前缩放与平移）。 */
    private void onWindowGeometryChanged(boolean resized) {
        widget.setBounds(contentX(), contentY(), contentW(), contentH());
        if (!resized) return;
        double panX = widget.getViewport().getPanX();
        double panY = widget.getViewport().getPanY();
        rebuildLayout(true, false);
        // setLayout 会 centerPan，这里恢复缩放前的平移，避免视图乱跳
        widget.getViewport().setPan(panX, panY);
    }

    /** 初始视图自适应，使整张画布可见。 */
    protected void fitToView(TreeDiagramLayout layout) {
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

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        win.renderBackground(graphics);
        if (isEmptyModel()) {
            Component empty = emptyText();
            if (empty != null && contentH() >= 12) {
                graphics.text(font, empty, contentX() + (contentW() - font.width(empty)) / 2,
                        contentY() + contentH() / 2 - 4, EMPTY_TEXT_COLOR);
            }
        } else {
            widget.render(graphics, font, mouseX, mouseY);
        }
        win.renderChrome(graphics, font, title(), mouseX, mouseY);
    }

    @Override
    public void renderTooltips(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        if (isEmptyModel()) return;
        widget.renderTooltips(graphics, font, mouseX, mouseY);
    }

    // ------------------------------------------------------------------ 输入

    @Override
    public boolean contains(double mouseX, double mouseY) {
        return win.inside(mouseX, mouseY);
    }

    /** 标题栏拖动窗口；左/右/下边框与右下角缩放；内容区仍然用于平移画布。 */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (win.mouseClicked(mouseX, mouseY, button)) return true;
        return win.inside(mouseX, mouseY) && !isEmptyModel()
                && widget.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        DraggableWindow.DragAction windowAction = win.mouseDragged(mouseX, mouseY, button);
        if (windowAction != DraggableWindow.DragAction.NONE) {
            onWindowGeometryChanged(windowAction == DraggableWindow.DragAction.RESIZE);
            return true;
        }
        return !isEmptyModel() && widget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void onMouseReleased() {
        win.onMouseReleased();
        widget.mouseReleased(0, 0, 0);
    }

    /** 整个窗口范围内滚轮都能缩放；落在标题栏 / 边框上时锚点就近折算到内容区。 */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!win.inside(mouseX, mouseY)) return false;
        double anchorX = Mth.clamp(mouseX, contentX(), contentX() + contentW() - 1);
        double anchorY = Mth.clamp(mouseY, contentY(), contentY() + contentH() - 1);
        return widget.mouseScrolled(anchorX, anchorY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode) {
        return widget.keyPressed(keyCode);
    }
}
