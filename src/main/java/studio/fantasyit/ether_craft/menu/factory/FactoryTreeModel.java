package studio.fantasyit.ether_craft.menu.factory;

import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;
import studio.fantasyit.ether_craft.factory.EtherProcessChipManager;
import studio.fantasyit.ether_craft.item.ProcessChipItem;
import studio.fantasyit.ether_craft.recipe.factory.PathNode;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
import studio.fantasyit.ether_craft.register.Tags;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 工厂"当前布局"的树形视图模型。
 *
 * <p>纯客户端：直接读取菜单已同步的 {@code pathBelongings / pathDepth / pathDirection / pathMaxDepth}
 * 与容器内容，不新增任何网络包，下标约定与 {@link EtherProcessFactoryEntity} 一致（{@code [row][col]}）。
 *
 * <p>与运行期（{@code MultiStepBuilder}）保持一致：通路上出现**连接芯片**时，通路会被切成多个独立步骤
 * —— 下游部分是父配方，连接芯片上游的部分是独立子流程。因此这里按步骤切分，每个步骤各生成一个
 * {@link Group}（即"识别为多个单独配方"）：
 * <ul>
 *   <li>下游步骤里，连接芯片所在格会生成一个"边界节点"，表示该步骤的输入来自上游步骤；</li>
 *   <li>上游步骤是独立的一组节点，其输出节点展示中间产物（运行期会把中间产物预览写在连接芯片格上）。</li>
 * </ul>
 *
 * <p>只有含非空芯片的通路格才会成为节点，纯通路空格子在父子推导时被折叠。
 */
public record FactoryTreeModel(List<Group> groups) {
    public static final FactoryTreeModel EMPTY = new FactoryTreeModel(List.of());

    /**
     * 一个"被识别出的配方/步骤"。
     *
     * @param outputIndex 所属输出槽行号
     * @param stepIndex   同一输出下的步骤序号（0 = 最靠近输出的父步骤）
     * @param maxDepth    该输出通路的深度
     * @param nodes       该步骤的节点
     * @param outNodeId   输出节点 id（固定为 {@code out}）
     * @param attachRow   本步骤接入其父步骤（下游）的连接芯片所在行；-1 表示本步骤就是最下游步骤
     * @param attachCol   本步骤接入其父步骤（下游）的连接芯片所在列；-1 表示本步骤就是最下游步骤
     */
    public record Group(int outputIndex, int stepIndex, int maxDepth, List<Node> nodes,
                        String outNodeId, int attachRow, int attachCol) {
        /** 是否为上游子流程（输出会经连接芯片送入父步骤）。 */
        public boolean hasParentStep() {
            return attachRow >= 0 && attachCol >= 0;
        }
    }

    /**
     * 步骤内的树节点。
     *
     * @param id         步骤内唯一 id（{@code c<row>_<col>} / {@code in<row>} / {@code b<row>_<col>} / {@code out}）
     * @param row        网格行；输出节点为 -1
     * @param col        网格列；输出节点为 -1
     * @param depth      距输出的步数（越大越靠近输出）
     * @param isInput    是否为输入叶子（原始输入槽或上游步骤产物）
     * @param isBoundary 是否为"连接芯片"边界节点（输入来自上游步骤）
     * @param nextId     父节点 id（指向输出方向）；根节点为 null
     */
    public record Node(String id, int row, int col, int depth, boolean isInput, boolean isBoundary, String nextId) {
    }

    private static final int[][] DIRS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
    private static final String OUT_ID = "out";

    /** 挂在某个通路格上的工艺芯片所在位置（渲染时据此现场读取容器物品）。 */
    public record ChipRef(int row, int col) {
    }

    /**
     * 收集"作用于该通路格"的工艺芯片，语义与运行期 {@code EtherProcessorRecipeUtil.scanForTrees} 完全一致：
     * 普通工艺芯片默认 {@code separate=true}，是通路之外的"墙"，通过 {@code effectSide} 作用于**相邻**的通路格；
     * 只有 {@code effectSelf} 的芯片（连接芯片）才落在通路格自身上。
     * 因此节点物品必须从"通路格 + 4 邻格"里取，不能只看通路格自身。
     */
    public static List<ChipRef> chipsOf(EtherProcessFactoryEntity be, int row, int col) {
        List<ChipRef> chips = new ArrayList<>(4);
        for (int[] dir : DIRS) {
            int r2 = row + dir[0];
            int c2 = col + dir[1];
            if (r2 < 0 || r2 >= be.ROWS || c2 < 0 || c2 >= be.COLS) continue;
            if (isChipWith(be.internalContainer.getItem(r2 * be.COLS + c2), true))
                chips.add(new ChipRef(r2, c2));
        }
        if (isChipWith(be.internalContainer.getItem(row * be.COLS + col), false))
            chips.add(new ChipRef(row, col));
        return chips;
    }

    /**
     * 该格是否为一颗"对通路生效"的工艺芯片。
     * 与 {@code EtherProcessFactoryEntity.updateChips} 同口径：必须是工艺芯片物品，且不是分隔芯片；
     * {@code effect} 从已同步的 {@link EtherProcessChipManager} 取，取不到时退回默认值。
     *
     * @param side true 取 {@code effectSide}（作用于邻格），false 取 {@code effectSelf}（作用于自身所在格）
     */
    private static boolean isChipWith(ItemStack stack, boolean side) {
        if (stack.isEmpty() || !stack.is(Tags.PROCESS_CHIP) || ProcessChipItem.isSeparator(stack)) return false;
        EtherProcessChipManager.ProcessChipRecord record = EtherProcessChipManager.get(stack);
        EtherProcessChipManager.ProcessChipEffectConfig effect =
                record == null ? EtherProcessChipManager.ProcessChipEffectConfig.DEFAULT : record.effect();
        return side ? effect.effectSide() : effect.effectSelf();
    }

    public static FactoryTreeModel build(EtherProcessFactoryEntity be) {
        List<Group> groups = new ArrayList<>();
        for (int output = 0; output < be.ROWS; output++) {
            new OutputBuilder(be, output).build(groups);
        }
        return new FactoryTreeModel(List.copyOf(groups));
    }

    // ------------------------------------------------------------------ 构建

    private static final class OutputBuilder {
        private final EtherProcessFactoryEntity be;
        private final int outputIndex;
        private final int rows;
        private final int cols;
        private final int maxDepth;
        private final Map<Long, Integer> depthAt = new HashMap<>();
        private final Map<Long, String> chipNodeId = new HashMap<>();
        private final Set<Long> concatCells = new HashSet<>();
        private final Map<Long, Long> parentCell = new HashMap<>();
        private final Map<Long, List<Long>> children = new HashMap<>();
        private final List<Long> rawInputCells = new ArrayList<>();
        private int stepCounter;

        OutputBuilder(EtherProcessFactoryEntity be, int outputIndex) {
            this.be = be;
            this.outputIndex = outputIndex;
            this.rows = be.ROWS;
            this.cols = be.COLS;
            this.maxDepth = be.pathMaxDepth[outputIndex];
            collect();
        }

        private void collect() {
            List<Long> cells = new ArrayList<>();
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    if (be.pathBelongings[row][col] != outputIndex) continue;
                    long key = key(row, col);
                    depthAt.put(key, be.pathDepth[row][col]);
                    cells.add(key);
                    ItemStack stack = be.internalContainer.getItem(row * cols + col);
                    if (ProcessChipItem.isConcat(stack)) {
                        // 连接芯片 separate=false、effectSelf=true，自己就在通路上：作为步骤边界而非普通芯片节点
                        concatCells.add(key);
                    } else if (!chipsOf(be, row, col).isEmpty()) {
                        // 普通工艺芯片是通路之外的"墙"，挂在通路格旁边；节点仍落在通路格上
                        chipNodeId.put(key, "c" + row + "_" + col);
                    }
                    if (col == 0 && PathNode.isDirect(PathNode.Direction.LEFT, be.pathDirection[row][col]))
                        rawInputCells.add(key);
                }
            }
            // 父子关系：父格 = depth + 1 的邻接格（同一通路内唯一）
            for (long key : cells) {
                int row = row(key);
                int col = col(key);
                int depth = depthAt.get(key);
                for (int[] dir : DIRS) {
                    int r2 = row + dir[0];
                    int c2 = col + dir[1];
                    if (r2 < 0 || r2 >= rows || c2 < 0 || c2 >= cols) continue;
                    long nk = key(r2, c2);
                    Integer nd = depthAt.get(nk);
                    if (nd != null && nd == depth + 1) {
                        parentCell.put(key, nk);
                        children.computeIfAbsent(nk, k -> new ArrayList<>()).add(key);
                        break;
                    }
                }
            }
        }

        void build(List<Group> out) {
            if (depthAt.isEmpty()) return;
            long root = -1;
            int best = Integer.MIN_VALUE;
            for (Map.Entry<Long, Integer> entry : depthAt.entrySet()) {
                if (entry.getValue() == maxDepth && !parentCell.containsKey(entry.getKey())) {
                    root = entry.getKey();
                    break;
                }
                if (entry.getValue() > best) {
                    best = entry.getValue();
                    root = entry.getKey();
                }
            }
            if (root < 0) return;
            buildStep(root, -1, out);
        }

        /**
         * 构建一个步骤。
         *
         * @param root     步骤起点格（子步骤时即连接芯片所在格）
         * @param boundary 本步骤与父步骤的连接芯片格；-1 表示这是最下游的父步骤
         */
        private void buildStep(long root, long boundary, List<Group> out) {
            Map<Long, String> nodeIds = new LinkedHashMap<>();
            List<Long> boundaries = new ArrayList<>();
            Set<Long> visited = new HashSet<>();
            Deque<Long> queue = new ArrayDeque<>();
            queue.add(root);
            visited.add(root);
            while (!queue.isEmpty()) {
                long key = queue.poll();
                boolean stepRootIsBoundary = boundary >= 0 && key == root;
                if (concatCells.contains(key) && !stepRootIsBoundary) {
                    boundaries.add(key);
                    continue;
                }
                if (!concatCells.contains(key) && chipNodeId.containsKey(key)) nodeIds.put(key, chipNodeId.get(key));
                for (long child : children.getOrDefault(key, List.of())) {
                    if (visited.add(child)) queue.add(child);
                }
            }

            List<Node> nodes = new ArrayList<>();
            nodes.add(new Node(OUT_ID, -1, -1, maxDepth + 1, false, false, null));
            for (long key : nodeIds.keySet()) {
                nodes.add(new Node(nodeIds.get(key), row(key), col(key), depthAt.get(key),
                        false, false, resolveParent(key, root, nodeIds)));
            }
            boolean hasRawInput = false;
            for (long key : rawInputCells) {
                if (!visited.contains(key)) continue;
                hasRawInput = true;
                nodes.add(new Node("in" + row(key), row(key), col(key), depthAt.get(key), true, false,
                        nodeIds.getOrDefault(key, resolveParent(key, root, nodeIds))));
            }
            for (long key : boundaries) {
                nodes.add(new Node("b" + row(key) + "_" + col(key), row(key), col(key), depthAt.get(key),
                        true, true, resolveAttach(key, nodeIds)));
            }

            if (!nodeIds.isEmpty() || hasRawInput || !boundaries.isEmpty()) {
                nodes.sort(Comparator.comparingInt(Node::depth)
                        .thenComparingInt(Node::row)
                        .thenComparingInt(Node::col));
                int attachRow = boundary < 0 ? -1 : row(boundary);
                int attachCol = boundary < 0 ? -1 : col(boundary);
                out.add(new Group(outputIndex, stepCounter++, maxDepth, List.copyOf(nodes),
                        OUT_ID, attachRow, attachCol));
            }

            for (long key : boundaries) {
                buildStep(key, key, out);
            }
        }

        /** 沿 depth + 1 方向逐格上溯，跳过空格子；越过步骤边界或到达步骤起点则挂到输出节点。 */
        private String resolveParent(long key, long stepRoot, Map<Long, String> nodeIds) {
            long current = key;
            while (true) {
                if (current == stepRoot) return OUT_ID;
                Long parent = parentCell.get(current);
                if (parent == null) return OUT_ID;
                if (concatCells.contains(parent)) return OUT_ID;
                String node = nodeIds.get(parent);
                if (node != null) return node;
                current = parent;
            }
        }

        /** 连接芯片格向下游找最近的本步骤芯片节点，作为边界节点的父节点。 */
        private String resolveAttach(long concatKey, Map<Long, String> nodeIds) {
            long current = concatKey;
            while (true) {
                Long parent = parentCell.get(current);
                if (parent == null) return OUT_ID;
                if (concatCells.contains(parent)) return OUT_ID;
                String node = nodeIds.get(parent);
                if (node != null) return node;
                current = parent;
            }
        }

        private long key(int row, int col) {
            return (long) row * cols + col;
        }

        private int row(long key) {
            return (int) (key / cols);
        }

        private int col(long key) {
            return (int) (key % cols);
        }
    }

    // ------------------------------------------------------------------ 签名

    /** 结构签名：只有它变化时才需要重建布局。进度 / 以太 / 物品数量不参与。 */
    public static long structureSignature(EtherProcessFactoryEntity be) {
        long h = 1125899906842597L;
        int rows = be.ROWS;
        int cols = be.COLS;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int belong = be.pathBelongings[row][col];
                h = 31 * h + belong;
                h = 31 * h + be.pathDepth[row][col];
                h = 31 * h + be.pathDirection[row][col];
                // 工艺芯片位于通路格之外的邻格，因此所有格子的容器内容都要参与签名，
                // 否则芯片增删/更换不会触发重建。
                ItemStack stack = be.internalContainer.getItem(row * cols + col);
                h = 31 * h + (stack.isEmpty() ? 0 : stack.getItem().hashCode());
                h = 31 * h + Objects.hashCode(stack.get(DataComponentRegistry.CHIP_ID));
            }
        }
        for (int i = 0; i < rows; i++)
            h = 31 * h + be.pathMaxDepth[i];
        return h;
    }
}
