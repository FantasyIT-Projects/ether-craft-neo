package studio.fantasyit.ether_craft.recipe.factory.render.widget;

import net.minecraft.util.Mth;

public class TreeDiagramViewport {
    double panX, panY;
    double zoom = 1.0;
    final double minZoom;
    int canvasWidth, canvasHeight;
    final int viewWidth, viewHeight;

    public TreeDiagramViewport(int canvasWidth, int canvasHeight, int viewWidth, int viewHeight) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.minZoom = Math.min(1.0, Math.min(
                (double) viewWidth / canvasWidth,
                (double) viewHeight / canvasHeight));
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

    public void zoomAt(double screenX, double screenY, double factor) {
        double oldZoom = zoom;
        double newZoom = Mth.clamp(zoom * factor, minZoom, 1.0);
        if (newZoom == oldZoom) return;
        double ratio = newZoom / oldZoom;
        panX = screenX - (screenX - panX) * ratio;
        panY = screenY - (screenY - panY) * ratio;
        zoom = newZoom;
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
