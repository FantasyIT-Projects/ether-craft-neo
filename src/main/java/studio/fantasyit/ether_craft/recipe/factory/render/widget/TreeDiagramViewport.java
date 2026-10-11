package studio.fantasyit.ether_craft.recipe.factory.render.widget;

import net.minecraft.util.Mth;

public class TreeDiagramViewport {
    double panX, panY;
    double zoom = 1.0;
    double minZoom;
    double maxZoom = 1.0;
    int canvasWidth, canvasHeight;
    int viewWidth, viewHeight;

    public TreeDiagramViewport(int canvasWidth, int canvasHeight, int viewWidth, int viewHeight) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.minZoom = computeMinZoom();
    }

    private double computeMinZoom() {
        double byWidth = canvasWidth <= 0 ? 1.0 : (double) viewWidth / canvasWidth;
        double byHeight = canvasHeight <= 0 ? 1.0 : (double) viewHeight / canvasHeight;
        return Math.min(1.0, Math.min(byWidth, byHeight));
    }

    /**
     * 布局尺寸确定后重算最小缩放。
     * {@link TreeDiagramWidget} 先以 0×0 画布构造，必须在 {@code setLayout} 时刷新，否则永远无法缩小。
     */
    public void setCanvasSize(int canvasWidth, int canvasHeight) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.minZoom = computeMinZoom();
        if (zoom < minZoom) zoom = minZoom;
        clampPan();
    }

    /**
     * 浮层窗口缩放后更新可视尺寸（保留当前缩放与平移，只做 clamp）。
     * JEI 侧不调用，行为不变。
     */
    public void setViewSize(int viewWidth, int viewHeight) {
        this.viewWidth = Math.max(1, viewWidth);
        this.viewHeight = Math.max(1, viewHeight);
        this.minZoom = computeMinZoom();
        if (zoom < minZoom) zoom = minZoom;
        clampPan();
    }

    public double toWorldX(double screenX) {
        return (screenX - panX) / zoom;
    }

    public double toWorldY(double screenY) {
        return (screenY - panY) / zoom;
    }

    public double toScreenX(double worldX) {
        return worldX * zoom + panX;
    }

    public double toScreenY(double worldY) {
        return worldY * zoom + panY;
    }

    public void clampPan() {
        double lowerX = Math.min(0, viewWidth - canvasWidth * zoom);
        double upperX = Math.max(0, viewWidth - canvasWidth * zoom);
        double lowerY = Math.min(0, viewHeight - canvasHeight * zoom);
        double upperY = Math.max(0, viewHeight - canvasHeight * zoom);
        panX = Mth.clamp(panX, lowerX, upperX);
        panY = Mth.clamp(panY, lowerY, upperY);
    }

    /** 设置放大上限（默认 1.0：只能缩到刚好显示整张画布，不能放大）。 */
    public void setMaxZoom(double maxZoom) {
        this.maxZoom = maxZoom;
        if (zoom > this.maxZoom) zoom = this.maxZoom;
        clampPan();
    }

    public double getMaxZoom() {
        return maxZoom;
    }

    public void zoomAt(double screenX, double screenY, double factor) {
        double oldZoom = zoom;
        double newZoom = Mth.clamp(zoom * factor, minZoom, Math.max(minZoom, maxZoom));
        if (newZoom == oldZoom) return;
        double ratio = newZoom / oldZoom;
        panX = screenX - (screenX - panX) * ratio;
        panY = screenY - (screenY - panY) * ratio;
        zoom = newZoom;
        clampPan();
    }

    /** 恢复平移位置（会 clamp 到合法范围）；用于窗口缩放后不跳回原点。 */
    public void setPan(double panX, double panY) {
        this.panX = panX;
        this.panY = panY;
        clampPan();
    }

    public void centerPan() {
        if (canvasWidth < viewWidth) {
            panX = (double) (viewWidth - canvasWidth) / 2.0;
        } else {
            panX = 0;
        }
        if (canvasHeight < viewHeight) {
            panY = (double) (viewHeight - canvasHeight) / 2.0;
        } else {
            panY = 0;
        }
    }

    public double getPanX() { return panX; }
    public double getPanY() { return panY; }
    public double getZoom() { return zoom; }
}
