package studio.fantasyit.ether_craft.recipe.factory.render.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;

import java.util.List;

public final class EdgeBatchRenderer {
    private static final int LINE_COLOR = 0xFFAAAAAA;

    public static void render(GuiGraphicsExtractor g, List<TreeDiagramLayout.Edge> edges) {
        for (var edge : edges) {
            drawLine(g, edge.fromX(), edge.fromY(), edge.toX(), edge.toY());
        }
    }

    public static void render(GuiGraphicsExtractor g, List<TreeDiagramLayout.Edge> edges, int lineColor) {
        for (var edge : edges) {
            drawLine(g, edge.fromX(), edge.fromY(), edge.toX(), edge.toY(), lineColor);
        }
    }

    private static void drawLine(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2) {
        drawLine(g, x1, y1, x2, y2, LINE_COLOR);
    }

    private static void drawLine(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        int midX = (x1 + x2) / 2;
        g.horizontalLine(x1, midX, y1, color);
        g.verticalLine(midX, y1, y2, color);
        g.horizontalLine(midX, x2, y2, color);
    }
}
