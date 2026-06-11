package studio.fantasyit.ether_craft.recipe.factory.render.data;

import java.util.ArrayList;
import java.util.List;

public final class TreeDiagramLayout {
    public final List<PositionedNode> nodes = new ArrayList<>();
    public final List<Edge> edges = new ArrayList<>();
    public PositionedOutput output;
    public int canvasWidth;
    public int canvasHeight;

    public record PositionedNode(
            String id,
            int x, int y,
            int width, int height,
            int midY,
            int exitX
    ) {
    }

    public record PositionedOutput(
            int x, int y,
            int width, int height,
            int midY
    ) {
    }

    public record Edge(int fromX, int fromY, int toX, int toY) {
    }
}
