package studio.fantasyit.ether_craft.integration.jei;

import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.DisplayContentsFactory;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeJson;

import java.util.*;

public class TreeLayout {
    static final int SLOT_SIZE = 18;
    static final int SLOT_SIZE_OUTPUT = 22;
    static final int CHIP_GAP = 1;
    static final int NODE_GAP = 15;
    static final int PADDING = 4;
    static final int WIDTH = 140;
    static final int HEIGHT = 90;
    static final int MIN_SPACING = 26;

    record Entry(String id, int x, int y, SizedIngredient ingredient) {
    }

    record ChipEntry(String parentId, int x, int y, SizedIngredient ingredient) {
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

        Map<String, Boolean> isInput = new HashMap<>();
        Set<String> allIds = new HashSet<>();
        for (var in : json.input()) {
            allIds.add(in.id());
            isInput.put(in.id(), true);
        }
        for (var proc : json.process()) {
            allIds.add(proc.id());
            isInput.put(proc.id(), false);
        }

        Map<String, Integer> levels = computeLevels(json, allIds);
        int maxLevel = levels.values().stream().max(Integer::compareTo).orElse(0);

        Map<String, Integer> nodeH = new HashMap<>();
        for (var in : json.input())
            nodeH.put(in.id(), SLOT_SIZE);
        for (var proc : json.process()) {
            int cnt = proc.item().size();
            nodeH.put(proc.id(), cnt * SLOT_SIZE + (cnt - 1) * CHIP_GAP - (cnt - 1) * 9);
        }

        Map<String, String> nextMap = new HashMap<>();
        for (var in : json.input())
            nextMap.put(in.id(), in.next());
        for (var proc : json.process())
            nextMap.put(proc.id(), proc.next());


        int outCount = json.output().item().size();
        int outHeight = outCount * SLOT_SIZE + Math.max(0, outCount - 1) * CHIP_GAP;  // 纵向总高度
        int outWidth = SLOT_SIZE;  // 固定宽度（一个槽位宽度）
        int usable = WIDTH - 2 * PADDING - outWidth;
        int spacing = maxLevel > 0 ? Math.max(MIN_SPACING, usable / maxLevel) : 0;
        layout.canvasWidth = PADDING + maxLevel * spacing + outWidth + PADDING;

        Map<Integer, List<String>> byLevel = new TreeMap<>();
        for (var e : levels.entrySet()) {
            byLevel.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }

        Map<String, Integer> nodeX = new HashMap<>();
        Map<String, Integer> nodeY = new HashMap<>();

        for (var entry : byLevel.entrySet()) {
            int level = entry.getKey();
            int colX = PADDING + (maxLevel - level) * spacing;
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
                totalH += nodeH.get(id) + NODE_GAP;
            }
            if (totalH > 0) totalH -= NODE_GAP;

            int curY = PADDING + (HEIGHT - 2 * PADDING - totalH) / 2;
            for (String id : ids) {
                nodeX.put(id, colX);
                nodeY.put(id, curY);
                curY += nodeH.get(id) + NODE_GAP;
            }
        }

        layout.outputX = layout.canvasWidth - PADDING - outWidth;
        layout.outputY = PADDING + (HEIGHT - 2 * PADDING - outHeight) / 2;

        layout.canvasHeight = HEIGHT;

        for (var in : json.input()) {
            layout.inputs.add(new Entry(in.id(), nodeX.get(in.id()), nodeY.get(in.id()), in.item()));
        }
        for (var proc : json.process()) {
            int cx = nodeX.get(proc.id());
            int cy = nodeY.get(proc.id());
            for (var sized : proc.item()) {
                layout.chips.add(new ChipEntry(proc.id(), cx, cy, sized));
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
                ty = layout.outputY + outHeight / 2;
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
                ty = layout.outputY + outHeight / 2;
            }
            layout.edges.add(new Edge(fx, fy, tx, ty));
        }

        return layout;
    }

    public static List<ItemStack> resolveSizedIngredient(SizedIngredient sized, ContextMap context) {
        return sized.ingredient().display()
                .resolve(context, (DisplayContentsFactory.ForStacks<ItemStack>)
                        stack -> stack.copyWithCount(sized.count()))
                .toList();
    }

    private static int nodeMidY(boolean isInput, int h) {
        return isInput ? SLOT_SIZE / 2 : h / 2;
    }

    private static Map<String, Integer> computeLevels(EtherProcessRecipeJson json, Set<String> allIds) {
        Map<String, Integer> levels = new HashMap<>();
        Map<String, List<String>> predecessors = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>();

        for (var in : json.input()) {
            String next = in.next();
            if (next != null && allIds.contains(next))
                predecessors.computeIfAbsent(next, k -> new ArrayList<>()).add(in.id());
            else {
                levels.put(in.id(), 1);
                queue.add(in.id());
            }
        }
        for (var proc : json.process()) {
            String next = proc.next();
            if (next != null && allIds.contains(next))
                predecessors.computeIfAbsent(next, k -> new ArrayList<>()).add(proc.id());
            else {
                levels.put(proc.id(), 1);
                queue.add(proc.id());
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
