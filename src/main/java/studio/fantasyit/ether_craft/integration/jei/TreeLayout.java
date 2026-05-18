package studio.fantasyit.ether_craft.integration.jei;

import net.minecraft.world.item.crafting.Ingredient;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeJson;

import java.util.*;

public class TreeLayout {
    static final int SLOT_SIZE = 18;
    static final int CHIP_GAP = 1;
    static final int NODE_GAP = 6;
    static final int PADDING = 4;
    static final int WIDTH = 140;
    static final int HEIGHT = 90;
    static final int MIN_SPACING = 26;

    record Entry(String id, int x, int y, Ingredient ingredient) {
    }

    record ChipEntry(String parentId, int x, int y, Ingredient ingredient) {
    }

    record Edge(int fromX, int fromY, int toX, int toY) {
    }

    final List<Entry> inputs = new ArrayList<>();
    final List<ChipEntry> chips = new ArrayList<>();
    final List<Edge> edges = new ArrayList<>();
    int outputX, outputY;
    int canvasWidth;
    int canvasHeight;

    static TreeLayout compute(EtherProcessRecipeJson json) {
        TreeLayout layout = new TreeLayout();

        Set<String> allIds = new HashSet<>();
        Map<String, String> nextMap = new HashMap<>();
        Map<String, Boolean> isInput = new HashMap<>();

        for (var in : json.input()) {
            allIds.add(in.id());
            nextMap.put(in.id(), in.next());
            isInput.put(in.id(), true);
        }
        for (var proc : json.process()) {
            allIds.add(proc.id());
            nextMap.put(proc.id(), proc.next());
            isInput.put(proc.id(), false);
        }

        Map<String, Integer> levels = new HashMap<>();
        for (String id : allIds) {
            computeLevel(id, nextMap, allIds, levels);
        }
        int maxLevel = levels.values().stream().max(Integer::compare).orElse(0);

        int outCount = json.output().item().size();
        int outWidth = outCount * SLOT_SIZE + Math.max(0, outCount - 1) * 2;
        int usable = WIDTH - 2 * PADDING - outWidth;
        int spacing = maxLevel > 0 ? Math.max(MIN_SPACING, usable / maxLevel) : 0;
        layout.canvasWidth = PADDING + maxLevel * spacing + outWidth + PADDING;

        Map<Integer, List<String>> byLevel = new TreeMap<>();
        for (var e : levels.entrySet()) {
            byLevel.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }

        Map<String, Integer> nodeX = new HashMap<>();
        Map<String, Integer> nodeY = new HashMap<>();
        Map<String, Integer> nodeH = new HashMap<>();

        for (var entry : byLevel.entrySet()) {
            int level = entry.getKey();
            int colX = PADDING + (maxLevel - level) * spacing;
            List<String> ids = entry.getValue();

            int totalH = 0;
            for (String id : ids) {
                int h;
                if (isInput.get(id)) {
                    h = SLOT_SIZE;
                } else {
                    int cnt = chipCount(json, id);
                    h = cnt * SLOT_SIZE + (cnt - 1) * CHIP_GAP;
                }
                nodeH.put(id, h);
                totalH += h + NODE_GAP;
            }
            totalH -= NODE_GAP;

            int curY = PADDING + (HEIGHT - 2 * PADDING - totalH) / 2;
            for (String id : ids) {
                nodeX.put(id, colX);
                nodeY.put(id, curY);
                curY += nodeH.get(id) + NODE_GAP;
            }
        }

        layout.outputX = layout.canvasWidth - PADDING - outWidth;
        layout.outputY = (HEIGHT - SLOT_SIZE) / 2;

        layout.canvasHeight = HEIGHT;

        for (var in : json.input()) {
            layout.inputs.add(new Entry(in.id(), nodeX.get(in.id()), nodeY.get(in.id()), in.item().ingredient()));
        }
        for (var proc : json.process()) {
            int cx = nodeX.get(proc.id());
            int cy = nodeY.get(proc.id());
            for (var sized : proc.item()) {
                layout.chips.add(new ChipEntry(proc.id(), cx, cy, sized.ingredient()));
                cy += SLOT_SIZE + CHIP_GAP;
            }
        }

        for (var in : json.input()) {
            int fx = nodeX.get(in.id()) + SLOT_SIZE;
            int fy = nodeY.get(in.id()) + SLOT_SIZE / 2;
            String next = in.next();
            int tx, ty;
            if (next != null && allIds.contains(next)) {
                tx = nodeX.get(next);
                ty = nodeY.get(next) + nodeMidY(isInput.get(next), nodeH.get(next));
            } else {
                tx = layout.outputX;
                ty = layout.outputY + SLOT_SIZE / 2;
            }
            layout.edges.add(new Edge(fx, fy, tx, ty));
        }
        for (var proc : json.process()) {
            int fx = nodeX.get(proc.id()) + SLOT_SIZE;
            int fy = nodeY.get(proc.id()) + nodeMidY(false, nodeH.get(proc.id()));
            String next = proc.next();
            int tx, ty;
            if (next != null && allIds.contains(next)) {
                tx = nodeX.get(next);
                ty = nodeY.get(next) + nodeMidY(isInput.get(next), nodeH.get(next));
            } else {
                tx = layout.outputX;
                ty = layout.outputY + SLOT_SIZE / 2;
            }
            layout.edges.add(new Edge(fx, fy, tx, ty));
        }

        return layout;
    }

    private static int nodeMidY(boolean isInput, int h) {
        return isInput ? SLOT_SIZE / 2 : h / 2;
    }

    private static int chipCount(EtherProcessRecipeJson json, String id) {
        for (var p : json.process()) {
            if (p.id().equals(id)) return p.item().size();
        }
        return 1;
    }

    private static int computeLevel(String id, Map<String, String> nextMap,
                                    Set<String> allIds, Map<String, Integer> levels) {
        if (levels.containsKey(id)) return levels.get(id);
        String next = nextMap.getOrDefault(id, null);
        int lvl;
        if (next == null || !allIds.contains(next)) {
            lvl = 1;
        } else {
            lvl = computeLevel(next, nextMap, allIds, levels) + 1;
        }
        levels.put(id, lvl);
        return lvl;
    }
}
