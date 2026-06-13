package studio.fantasyit.ether_craft.recipe.factory.render.data;

import java.util.*;

public final class TreeLayoutCalculator {

    private TreeLayoutCalculator() {
    }

    public static TreeDiagramLayout compute(TreeDiagramSpec spec) {
        TreeDiagramLayout layout = new TreeDiagramLayout();
        TreeDiagramSpec.LayoutConfig cfg = spec.config();

        Set<String> allIds = new HashSet<>();
        Map<String, Boolean> isInput = new HashMap<>();
        Map<String, String> nextMap = new HashMap<>();
        for (var node : spec.nodes()) {
            allIds.add(node.id());
            isInput.put(node.id(), node.isInput());
            nextMap.put(node.id(), node.nextId());
        }

        Map<String, Integer> nodeHeights = new HashMap<>();
        for (var node : spec.nodes()) {
            if (node.isInput()) {
                nodeHeights.put(node.id(), cfg.slotSize());
            } else {
                int cnt = node.subSlotCount();
                nodeHeights.put(node.id(),
                        cnt * cfg.slotSize() + (cnt - 1) * cfg.chipGap() - (cnt - 1) * cfg.chipOverlap());
            }
        }

        Map<String, Integer> levels = computeLevels(spec.nodes(), allIds);

        int maxLevel = levels.values().stream().max(Integer::compareTo).orElse(0);

        int outCount = spec.output().slotCount();
        int outHeight = outCount * cfg.slotSize() + Math.max(0, outCount - 1) * cfg.chipGap();
        int outWidth = cfg.slotSize();
        int usable = cfg.viewWidth() - 2 * cfg.padding() - outWidth;
        int spacing = maxLevel > 0 ? Math.max(cfg.minSpacing(), usable / maxLevel) : 0;
        layout.canvasWidth = cfg.padding() + maxLevel * spacing + outWidth + cfg.padding();

        Map<Integer, List<String>> byLevel = new TreeMap<>();
        for (var e : levels.entrySet()) {
            byLevel.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }

        int maxColumnContentHeight = 0;
        for (var entry : byLevel.entrySet()) {
            int totalH = 0;
            for (String id : entry.getValue()) {
                totalH += nodeHeights.get(id) + cfg.nodeGap();
            }
            if (totalH > 0) totalH -= cfg.nodeGap();
            maxColumnContentHeight = Math.max(maxColumnContentHeight, totalH);
        }
        layout.canvasHeight = Math.max(cfg.viewHeight(), maxColumnContentHeight + 2 * cfg.padding());

        Map<String, Integer> nodeX = new HashMap<>();
        Map<String, Integer> nodeY = new HashMap<>();

        for (var entry : byLevel.entrySet()) {
            int level = entry.getKey();
            int colX = cfg.padding() + (maxLevel - level) * spacing;
            List<String> ids = new ArrayList<>(entry.getValue());

            if (level == 1) {
                ids.sort(String::compareTo);
            } else {
                ids.sort((a, b) -> {
                    String na = nextMap.get(a);
                    String nb = nextMap.get(b);
                    int ya = na != null && nodeY.containsKey(na) ? nodeY.get(na) : 0;
                    int yb = nb != null && nodeY.containsKey(nb) ? nodeY.get(nb) : 0;
                    if (ya != yb) return Integer.compare(ya, yb);
                    return a.compareTo(b);
                });
            }

            int totalH = 0;
            for (String id : ids) {
                totalH += nodeHeights.get(id) + cfg.nodeGap();
            }
            if (totalH > 0) totalH -= cfg.nodeGap();

            int curY = cfg.padding() + (layout.canvasHeight - 2 * cfg.padding() - totalH) / 2;
            for (String id : ids) {
                nodeX.put(id, colX);
                nodeY.put(id, curY);
                curY += nodeHeights.get(id) + cfg.nodeGap();
            }
        }

        layout.output = new TreeDiagramLayout.PositionedOutput(
                layout.canvasWidth - cfg.padding() - outWidth,
                cfg.padding() + (layout.canvasHeight - 2 * cfg.padding() - outHeight) / 2,
                outWidth,
                outHeight,
                (outHeight + 1) / 2
        );

        for (var nodeSpec : spec.nodes()) {
            int h = nodeHeights.get(nodeSpec.id());
            int x = nodeX.get(nodeSpec.id());
            int y = nodeY.get(nodeSpec.id());
            int w = cfg.slotSize();
            layout.nodes.add(new TreeDiagramLayout.PositionedNode(
                    nodeSpec.id(), x, y, w, h,
                    isInput.get(nodeSpec.id()) ? cfg.slotSize() / 2 : (h + 1) / 2,
                    x + cfg.slotSize()
            ));
        }

        for (var nodeSpec : spec.nodes()) {
            int fx = nodeX.get(nodeSpec.id()) + cfg.slotSize();
            int fy = nodeY.get(nodeSpec.id())
                    + (isInput.get(nodeSpec.id()) ? cfg.slotSize() / 2 : (nodeHeights.get(nodeSpec.id()) + 1) / 2) - 2;
            String next = nodeSpec.nextId();
            int tx, ty;
            if (next != null && allIds.contains(next)) {
                tx = nodeX.get(next);
                ty = nodeY.get(next) + (isInput.get(next) ? cfg.slotSize() / 2 : (nodeHeights.get(next) + 1) / 2) - 2;
            } else {
                tx = layout.output.x();
                ty = layout.output.y() + (outHeight + 1) / 2 - 2;
            }
            layout.edges.add(new TreeDiagramLayout.Edge(fx, fy, tx, ty));
        }

        return layout;
    }

    private static Map<String, Integer> computeLevels(
            List<TreeDiagramSpec.NodeSpec> nodes, Set<String> allIds) {
        Map<String, Integer> levels = new HashMap<>();
        Map<String, List<String>> predecessors = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>();

        for (var node : nodes) {
            String next = node.nextId();
            if (next != null && allIds.contains(next)) {
                predecessors.computeIfAbsent(next, k -> new ArrayList<>()).add(node.id());
            } else {
                levels.put(node.id(), 1);
                queue.add(node.id());
            }
        }

        while (!queue.isEmpty()) {
            String id = queue.poll();
            int level = levels.get(id);
            for (String pred : predecessors.getOrDefault(id, List.of())) {
                if (!levels.containsKey(pred)) {
                    levels.put(pred, level + 1);
                    queue.add(pred);
                }
            }
        }

        for (String id : allIds) {
            levels.putIfAbsent(id, 1);
        }

        return levels;
    }
}
