package studio.fantasyit.ether_craft.recipe.factory.render.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 把"每个输出位置一棵树"的多份 {@link TreeDiagramLayout} 合成为一张画布，并给出分组表头位置。
 *
 * <p>纯数据层实现：不依赖任何 Minecraft 类型。每组内部沿用 {@link TreeLayoutCalculator} 的算法，
 * 随后按 {@code (0, yOffset)} 平移并加上组前缀，纵向堆叠；组内显式输出节点射向 {@code PositionedOutput}
 * 的那条边会被丢弃（输出以节点形式绘制，不使用槽位）。
 */
public final class TreeDiagramLayoutComposer {

    private TreeDiagramLayoutComposer() {
    }

    public record GroupSpec(String prefix, TreeDiagramSpec spec) {
    }

    public record Header(String prefix, int x, int y, int width, int height) {
    }

    public record Result(TreeDiagramLayout layout, List<Header> headers) {
    }

    public static Result compose(List<GroupSpec> groups, int viewWidth, int groupGap, int headerHeight) {
        TreeDiagramLayout merged = new TreeDiagramLayout();
        List<Header> headers = new ArrayList<>(groups.size());
        int width = Math.max(1, viewWidth);
        int yCursor = 0;

        for (GroupSpec group : groups) {
            TreeDiagramSpec spec = group.spec();
            TreeDiagramLayout groupLayout = TreeLayoutCalculator.compute(spec);
            TreeDiagramSpec.LayoutConfig cfg = spec.config();

            int outCount = spec.output().slotCount();
            int outHeight = outCount * cfg.slotSize() + Math.max(0, outCount - 1) * cfg.chipGap();
            // 与 TreeLayoutCalculator 里输出边锚点保持一致，用于丢弃"输出节点 -> 空输出槽"的悬空边
            int anchorX = groupLayout.output.x();
            int anchorY = groupLayout.output.y() + (outHeight + 1) / 2 - 2;

            int offsetY = yCursor + headerHeight;
            for (TreeDiagramLayout.PositionedNode node : groupLayout.nodes) {
                merged.nodes.add(new TreeDiagramLayout.PositionedNode(
                        group.prefix() + node.id(),
                        node.x(), node.y() + offsetY,
                        node.width(), node.height(),
                        node.midY(), node.exitX()));
            }
            for (TreeDiagramLayout.Edge edge : groupLayout.edges) {
                if (edge.toX() == anchorX && edge.toY() == anchorY) continue;
                merged.edges.add(new TreeDiagramLayout.Edge(
                        edge.fromX(), edge.fromY() + offsetY,
                        edge.toX(), edge.toY() + offsetY));
            }

            width = Math.max(width, groupLayout.canvasWidth);
            headers.add(new Header(group.prefix(), 0, yCursor, groupLayout.canvasWidth, headerHeight));
            yCursor += headerHeight + groupLayout.canvasHeight + groupGap;
        }

        List<Header> normalizedHeaders = new ArrayList<>(headers.size());
        for (Header header : headers) {
            normalizedHeaders.add(new Header(header.prefix(), header.x(), header.y(), width, header.height()));
        }

        merged.canvasWidth = width;
        merged.canvasHeight = Math.max(1, groups.isEmpty() ? 0 : yCursor - groupGap);
        merged.output = new TreeDiagramLayout.PositionedOutput(merged.canvasWidth, 0, 0, 0, 0);
        return new Result(merged, normalizedHeaders);
    }
}
