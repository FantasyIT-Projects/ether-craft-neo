package studio.fantasyit.ether_craft.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 通用浮动窗口：只负责窗口几何、拖动/缩放交互与窗口装饰（标题栏 / 边框 / 右下角抓手）。
 *
 * <p>内容完全由调用方负责：在 {@link #renderBackground} 之后、{@link #renderChrome} 之前，
 * 在自己的内容区（{@link #contentX()} / {@link #contentY()} / {@link #contentWidth()} /
 * {@link #contentHeight()}）里绘制，并按 {@link #inside} 决定是否接管鼠标。
 *
 * <ul>
 *   <li>标题栏（高 {@link #TITLE_HEIGHT}）左键拖拽 = 移动窗口，拖动时至少保留
 *       {@link #KEEP_VISIBLE} 像素在屏幕内，不会拖丢；</li>
 *   <li>左 / 右 / 下三条边框（{@link #RESIZE_BORDER} 带宽）与右下角抓手左键拖拽 = 缩放，
 *       下限由 {@link #setMinSize} 指定；标题栏不参与缩放，固定用于移动；</li>
 *   <li>没有高度下限，窗口再小也会照常绘制（由调用方自行裁剪内容）。</li>
 * </ul>
 */
public class DraggableWindow {
    /** 标题栏高度（拖动窗口的手柄区）。 */
    public static final int TITLE_HEIGHT = 12;
    /** 边框拖拽的判定带宽。 */
    private static final int RESIZE_BORDER = 4;
    /** 拖动 / 缩放时至少保留在屏幕内的像素。 */
    private static final int KEEP_VISIBLE = 32;
    /** 右下角抓手至少要有的宽度，避免窄窗口画出反向矩形。 */
    private static final int GRIP_MIN_WIDTH = 16;

    private static final int EDGE_BOTTOM = 1;
    private static final int EDGE_LEFT = 2;
    private static final int EDGE_RIGHT = 4;

    private static final int BG_COLOR = 0x40000000;
    private static final int BORDER_COLOR = 0x60FFFFFF;
    private static final int TITLE_BG = 0x70000000;
    private static final int TITLE_BG_HOVER = 0x90000000;
    private static final int TITLE_TEXT = 0xFFE6ECF5;
    private static final int TITLE_DIM = 0xFF8C94A6;
    private static final int GRIP_COLOR = 0x66FFFFFF;
    private static final int HOVER_COLOR = 0x50FFFFFF;

    /** 一次拖拽会话对窗口做的操作。 */
    public enum DragAction {
        /** 没有进行窗口拖动/缩放（事件未被消费）。 */
        NONE,
        /** 正在移动窗口。 */
        MOVE,
        /** 正在缩放窗口。 */
        RESIZE
    }

    private int x;
    private int y;
    private int width;
    private int height;
    private int minWidth = 48;
    private int minHeight = TITLE_HEIGHT + 4;
    private boolean showSize = true;

    private DragAction action = DragAction.NONE;
    private int dragEdge;
    private double moveOffX;
    private double moveOffY;
    private int startX;
    private int startY;
    private int startWidth;
    private int startHeight;
    private double startMouseX;
    private double startMouseY;

    public DraggableWindow(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(minWidth, width);
        this.height = Math.max(minHeight, height);
    }

    /** 设置缩放下限（内容高度仍然没有下限）。 */
    public DraggableWindow setMinSize(int minWidth, int minHeight) {
        this.minWidth = Math.max(8, minWidth);
        this.minHeight = Math.max(1, minHeight);
        this.width = Math.max(this.minWidth, this.width);
        this.height = Math.max(this.minHeight, this.height);
        return this;
    }

    /** 是否在标题栏右侧显示当前窗口尺寸。 */
    public DraggableWindow setShowSize(boolean showSize) {
        this.showSize = showSize;
        return this;
    }

    // ------------------------------------------------------------------ 几何

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    /** 内容区（标题栏以下）左上角与尺寸。 */
    public int contentX() {
        return x;
    }

    public int contentY() {
        return y + TITLE_HEIGHT;
    }

    public int contentWidth() {
        return Math.max(1, width);
    }

    public int contentHeight() {
        return Math.max(1, height - TITLE_HEIGHT);
    }

    public boolean inside(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean inTitleBar(double mouseX, double mouseY) {
        return inside(mouseX, mouseY) && mouseY < y + TITLE_HEIGHT;
    }

    /** 是否正在拖动/缩放窗口（主机可据此把拖拽事件整体让给窗口）。 */
    public boolean isDragging() {
        return action != DragAction.NONE;
    }

    /** 鼠标落在哪个缩放手柄上；标题栏固定用于移动（0 = 不是手柄）。 */
    private int resizeEdgeAt(double mouseX, double mouseY) {
        if (!inside(mouseX, mouseY) || mouseY < y + TITLE_HEIGHT) return 0;
        int edge = 0;
        if (mouseY >= y + height - RESIZE_BORDER) edge |= EDGE_BOTTOM;
        if (mouseX < x + RESIZE_BORDER) edge |= EDGE_LEFT;
        if (mouseX >= x + width - RESIZE_BORDER) edge |= EDGE_RIGHT;
        return edge;
    }

    /** 拖动/缩放时至少保留一部分窗口在屏幕内。 */
    private void clampToScreen() {
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        x = Mth.clamp(x, -width + KEEP_VISIBLE, screenW - KEEP_VISIBLE);
        y = Mth.clamp(y, 0, Math.max(0, screenH - TITLE_HEIGHT));
    }

    // ------------------------------------------------------------------ 输入

    /** 按住标题栏开始移动、按住边框/抓手开始缩放；返回是否消费该次点击。 */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !inside(mouseX, mouseY)) return false;
        if (mouseY < y + TITLE_HEIGHT) {
            action = DragAction.MOVE;
            moveOffX = mouseX - x;
            moveOffY = mouseY - y;
            return true;
        }
        int edge = resizeEdgeAt(mouseX, mouseY);
        if (edge != 0) {
            action = DragAction.RESIZE;
            dragEdge = edge;
            startX = x;
            startY = y;
            startWidth = width;
            startHeight = height;
            startMouseX = mouseX;
            startMouseY = mouseY;
            return true;
        }
        return false;
    }

    /** 拖拽中：返回本次拖拽对窗口做的操作（{@code NONE} = 不消费，交回调用方）。 */
    public DragAction mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0) return DragAction.NONE;
        if (action == DragAction.MOVE) {
            x = (int) Math.round(mouseX - moveOffX);
            y = (int) Math.round(mouseY - moveOffY);
            clampToScreen();
            return DragAction.MOVE;
        }
        if (action == DragAction.RESIZE) {
            int dx = (int) Math.round(mouseX - startMouseX);
            int dy = (int) Math.round(mouseY - startMouseY);
            x = startX;
            y = startY;
            width = startWidth;
            height = startHeight;
            if ((dragEdge & EDGE_RIGHT) != 0) width = Math.max(minWidth, startWidth + dx);
            if ((dragEdge & EDGE_BOTTOM) != 0) height = Math.max(minHeight, startHeight + dy);
            if ((dragEdge & EDGE_LEFT) != 0) {
                width = Math.max(minWidth, startWidth - dx);
                x = startX + startWidth - width;
            }
            return DragAction.RESIZE;
        }
        return DragAction.NONE;
    }

    public void onMouseReleased() {
        action = DragAction.NONE;
    }

    // ------------------------------------------------------------------ 渲染

    /** 窗口底板，在内容之前绘制。 */
    public void renderBackground(GuiGraphicsExtractor graphics) {
        graphics.fill(x, y, x + width, y + height, BG_COLOR);
    }

    /** 窗口装饰，在内容之后绘制：标题栏、边框、右下角抓手与边框悬停高亮。 */
    public void renderChrome(GuiGraphicsExtractor graphics, Font font, Component title,
                             int mouseX, int mouseY) {
        int top = y + 1;
        int titleBottom = y + TITLE_HEIGHT;
        int innerRight = Math.max(x + 1, x + width - 1);
        boolean hovered = inTitleBar(mouseX, mouseY);
        graphics.fill(x + 1, top, innerRight, titleBottom, hovered ? TITLE_BG_HOVER : TITLE_BG);
        graphics.enableScissor(x + 1, top, innerRight, titleBottom);
        if (title != null) {
            graphics.text(font, title, x + 4, top + 2, TITLE_TEXT);
        }
        if (showSize) {
            String size = width + "×" + height;
            graphics.text(font, size, x + width - 4 - font.width(size), top + 2, TITLE_DIM);
        }
        graphics.disableScissor();

        int right = x + width - 1;
        int bottom = y + height - 1;
        if (width >= GRIP_MIN_WIDTH) {
            for (int i = 1; i <= 3; i++) {
                int o = i * 3;
                graphics.fill(right - o, bottom - 1, right - o + 1, bottom, GRIP_COLOR);
                graphics.fill(right - 1, bottom - o, right, bottom - o + 1, GRIP_COLOR);
            }
        }

        graphics.fill(x, y, x + width, y + 1, BORDER_COLOR);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER_COLOR);
        graphics.fill(x, y, x + 1, y + height, BORDER_COLOR);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER_COLOR);

        int edge = resizeEdgeAt(mouseX, mouseY);
        if (edge == 0) return;
        if ((edge & EDGE_BOTTOM) != 0) graphics.fill(x, bottom, x + width, bottom + 1, HOVER_COLOR);
        if ((edge & EDGE_LEFT) != 0) graphics.fill(x, y + TITLE_HEIGHT, x + 1, y + height, HOVER_COLOR);
        if ((edge & EDGE_RIGHT) != 0) graphics.fill(right, y + TITLE_HEIGHT, right + 1, y + height, HOVER_COLOR);
    }
}
