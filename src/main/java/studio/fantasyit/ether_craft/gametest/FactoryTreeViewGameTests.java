package studio.fantasyit.ether_craft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryBlock;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;
import studio.fantasyit.ether_craft.item.ProcessChipItem;
import studio.fantasyit.ether_craft.menu.factory.FactoryTreeModel;
import studio.fantasyit.ether_craft.recipe.factory.PathNode;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayoutComposer;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramSpec;
import studio.fantasyit.ether_craft.register.BlockRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工厂树形可视化的 GameTest：只覆盖纯逻辑部分（模型建树 + 布局合成），不涉及客户端渲染。
 *
 * <p>{@link FactoryTreeModel} 直接消费 {@code pathBelongings / pathDepth / pathDirection / pathMaxDepth}
 * 与内部容器，因此这里不放置任何方块，直接构造方块实体并写入这些数组来验证建树、折叠与分组。
 */
public final class FactoryTreeViewGameTests {
    private FactoryTreeViewGameTests() {
    }

    private static final int LEFT = 1 << PathNode.Direction.LEFT.ordinal();

    // ------------------------------------------------------------------
    // 测试 1：由 path 数据建树（父子关系 / 输入叶子 / 空格子折叠 / 空通路）
    // ------------------------------------------------------------------
    public static void treeModelFixture(GameTestHelper helper) {
        EtherProcessFactoryEntity be = newFactory();
        int cols = be.COLS;
        // 3x3：第 1 行是一条由空格构成的通路；工艺芯片是"墙"，挂在通路格的上下两侧（effectSide）
        clearPath(be);
        setPath(be, 1, 2, 0, 2, LEFT);
        setPath(be, 1, 1, 0, 1, LEFT);
        setPath(be, 1, 0, 0, 0, LEFT);
        be.pathMaxDepth[0] = 2;
        be.internalContainer.setItem(0 * cols + 1, chipStack("stamping_chip"));   // 挂在 (1,1) 上方
        be.internalContainer.setItem(2 * cols + 1, chipStack("cutting_chip"));    // 挂在 (1,1) 下方

        FactoryTreeModel model = FactoryTreeModel.build(be);
        helper.assertTrue(model.groups().size() == 1, "应恰好有一个输出分组，实际 " + model.groups().size());
        FactoryTreeModel.Group group = model.groups().get(0);
        helper.assertTrue(group.outputIndex() == 0, "分组输出行应为 0，实际 " + group.outputIndex());
        helper.assertTrue(group.maxDepth() == 2, "分组最大深度应为 2，实际 " + group.maxDepth());

        Map<String, FactoryTreeModel.Node> nodes = index(group);
        helper.assertTrue(nodes.size() == 3, "应包含 out + 1 个芯片节点 + 1 个输入，实际 " + nodes.keySet());
        helper.assertTrue(nodes.containsKey("out") && nodes.containsKey("c1_1") && nodes.containsKey("in1"),
                "节点集合不符合预期: " + nodes.keySet());
        helper.assertTrue(nodes.get("out").nextId() == null, "输出节点应为根（nextId == null）");
        helper.assertTrue("out".equals(nodes.get("c1_1").nextId()), "芯片节点应挂在输出节点上");
        helper.assertTrue(nodes.get("in1").isInput(), "in1 应为输入叶子");
        helper.assertTrue("c1_1".equals(nodes.get("in1").nextId()), "输入叶子应挂在芯片节点上");

        // 芯片挂在通路格之外的邻格：节点物品必须能从"通路格 + 邻格"里取到
        helper.assertTrue(FactoryTreeModel.chipsOf(be, 1, 1).size() == 2,
                "通路格 (1,1) 应收集到上下 2 颗邻格芯片，实际 " + FactoryTreeModel.chipsOf(be, 1, 1).size());
        helper.assertTrue(FactoryTreeModel.chipsOf(be, 1, 0).isEmpty(), "没有邻格芯片的通路格不应产生节点");

        // 拿掉一颗后只剩 1 颗
        be.internalContainer.setItem(0 * cols + 1, ItemStack.EMPTY);
        helper.assertTrue(FactoryTreeModel.chipsOf(be, 1, 1).size() == 1, "移除一颗后应只剩 1 颗");

        // 没有任何芯片 -> 通路格折叠，输入直接挂到输出
        be.internalContainer.setItem(2 * cols + 1, ItemStack.EMPTY);
        Map<String, FactoryTreeModel.Node> collapsed = index(FactoryTreeModel.build(be).groups().get(0));
        helper.assertTrue(!collapsed.containsKey("c1_1"), "没有芯片的通路格不应产生节点");
        helper.assertTrue("out".equals(collapsed.get("in1").nextId()),
                "没有芯片时应折叠，实际父节点为 " + collapsed.get("in1").nextId());

        // 分隔芯片与非工艺芯片物品都不算芯片
        be.internalContainer.setItem(0 * cols + 1, chipStack("separator_chip"));
        be.internalContainer.setItem(2 * cols + 1, new ItemStack(Items.IRON_INGOT));
        helper.assertTrue(FactoryTreeModel.chipsOf(be, 1, 1).isEmpty(),
                "分隔芯片与普通物品都不应被当作工艺芯片");
        be.internalContainer.setItem(0 * cols + 1, ItemStack.EMPTY);
        be.internalContainer.setItem(2 * cols + 1, ItemStack.EMPTY);

        // 无通路 -> 无分组
        clearPath(be);
        helper.assertTrue(FactoryTreeModel.build(be).groups().isEmpty(), "没有通路时不应产生分组");

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 测试 2：结构签名（决定是否需要重建布局）
    // ------------------------------------------------------------------
    public static void treeModelSignature(GameTestHelper helper) {
        EtherProcessFactoryEntity be = newFactory();
        int cols = be.COLS;
        clearPath(be);
        setPath(be, 1, 2, 0, 1, LEFT);

        long base = FactoryTreeModel.structureSignature(be);
        helper.assertTrue(FactoryTreeModel.structureSignature(be) == base, "结构未变化时签名应保持稳定");

        be.pathDepth[1][2] = 5;
        helper.assertTrue(FactoryTreeModel.structureSignature(be) != base, "路径深度变化后签名应变化");
        be.pathDepth[1][2] = 1;

        // 芯片挂在通路格之外的邻格，也必须参与签名，否则芯片增删不会触发重建
        be.internalContainer.setItem(0 * cols + 2, chipStack("stamping_chip"));
        helper.assertTrue(FactoryTreeModel.structureSignature(be) != base, "邻格芯片变化后签名应变化");
        be.internalContainer.setItem(0 * cols + 2, ItemStack.EMPTY);

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 测试 3：多分组布局合成（前缀 / 纵向堆叠 / 悬空输出边丢弃）
    // ------------------------------------------------------------------
    public static void treeLayoutCompose(GameTestHelper helper) {
        TreeDiagramSpec single = spec(
                new TreeDiagramSpec.NodeSpec("out", 1, null, false),
                new TreeDiagramSpec.NodeSpec("c", 1, "out", false),
                new TreeDiagramSpec.NodeSpec("in", 1, "c", true));
        TreeDiagramSpec chain = spec(
                new TreeDiagramSpec.NodeSpec("out", 1, null, false),
                new TreeDiagramSpec.NodeSpec("c2", 1, "out", false),
                new TreeDiagramSpec.NodeSpec("c1", 1, "c2", false),
                new TreeDiagramSpec.NodeSpec("in", 1, "c1", true));

        TreeDiagramLayoutComposer.Result result = TreeDiagramLayoutComposer.compose(
                List.of(new TreeDiagramLayoutComposer.GroupSpec("a_", single),
                        new TreeDiagramLayoutComposer.GroupSpec("b_", chain)),
                140, 6, 18);

        helper.assertTrue(result.headers().size() == 2, "应有两个分组表头，实际 " + result.headers().size());
        helper.assertTrue(result.headers().get(1).y() > result.headers().get(0).y(),
                "第二个分组表头应位于第一个分组下方");
        helper.assertTrue(result.headers().get(0).width() == result.headers().get(1).width(),
                "两个表头宽度应统一为合并后画布宽度");

        TreeDiagramLayout layout = result.layout();
        helper.assertTrue(layout.nodes.size() == 7, "合并后应有 7 个节点，实际 " + layout.nodes.size());
        // 每组显式输出节点射向空输出槽的边应被丢弃：(3-1) + (4-1) = 5
        helper.assertTrue(layout.edges.size() == 5, "合并后应有 5 条边，实际 " + layout.edges.size());
        for (TreeDiagramLayout.PositionedNode node : layout.nodes) {
            helper.assertTrue(node.id().startsWith("a_") || node.id().startsWith("b_"),
                    "节点 id 应带分组前缀: " + node.id());
        }

        int firstGroupBottom = Integer.MIN_VALUE;
        int secondGroupTop = Integer.MAX_VALUE;
        for (TreeDiagramLayout.PositionedNode node : layout.nodes) {
            if (node.id().startsWith("a_")) firstGroupBottom = Math.max(firstGroupBottom, node.y());
            else secondGroupTop = Math.min(secondGroupTop, node.y());
        }
        helper.assertTrue(secondGroupTop > firstGroupBottom, "第二个分组的所有节点都应位于第一个分组下方");
        helper.assertTrue(layout.canvasWidth >= 140, "画布宽度不应小于视口宽度，实际 " + layout.canvasWidth);
        helper.assertTrue(layout.canvasHeight > 0, "画布高度应为正，实际 " + layout.canvasHeight);

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 测试 4：连接芯片把通路切成多个独立配方
    // ------------------------------------------------------------------
    public static void treeModelConcatSplit(GameTestHelper helper) {
        EtherProcessFactoryEntity be = newFactory(2);
        int cols = be.COLS;
        clearPath(be);
        // 5x5：通路是一条空行（第 2 行），col4 最靠近输出（pathDepth 最大），col0 为输入叶子。
        // 工艺芯片是"墙"，挂在通路格的相邻格（effectSide）；连接芯片 separate=false，自己就落在通路上。
        for (int col = 0; col <= 4; col++)
            setPath(be, 2, col, 0, col, LEFT);
        be.pathMaxDepth[0] = 4;
        be.internalContainer.setItem(2 * cols + 2, ProcessChipItem.getStackFor(ProcessChipItem.CONCAT));
        be.internalContainer.setItem(1 * cols + 3, chipStack("stamping_chip"));   // 挂在 (2,3) 上方
        be.internalContainer.setItem(1 * cols + 4, chipStack("stamping_chip"));   // 挂在 (2,4) 上方
        be.internalContainer.setItem(1 * cols + 1, chipStack("stamping_chip"));   // 挂在 (2,1) 上方

        FactoryTreeModel model = FactoryTreeModel.build(be);
        helper.assertTrue(model.groups().size() == 2,
                "含连接芯片的通路应被识别为 2 个独立配方，实际 " + model.groups().size());

        FactoryTreeModel.Group downstream = model.groups().getFirst();
        FactoryTreeModel.Group upstream = model.groups().get(1);
        helper.assertTrue(downstream.stepIndex() == 0 && upstream.stepIndex() == 1,
                "步骤序号应为 0/1，实际 " + downstream.stepIndex() + "/" + upstream.stepIndex());
        helper.assertTrue(!downstream.hasParentStep(),
                "最下游步骤不应有父步骤，实际 " + downstream.attachRow() + "," + downstream.attachCol());
        helper.assertTrue(upstream.hasParentStep() && upstream.attachRow() == 2 && upstream.attachCol() == 2,
                "上游步骤应记录其接入的连接芯片位置，实际 " + upstream.attachRow() + "," + upstream.attachCol());

        Map<String, FactoryTreeModel.Node> down = index(downstream);
        helper.assertTrue(down.keySet().containsAll(List.of("out", "c2_4", "c2_3", "b2_2")),
                "下游步骤节点集合不符合预期: " + down.keySet());
        helper.assertTrue(!down.containsKey("c2_1") && !down.containsKey("in2"),
                "上游步骤的芯片/输入不应出现在下游步骤: " + down.keySet());
        helper.assertTrue(down.get("b2_2").isBoundary() && down.get("b2_2").isInput(),
                "连接芯片格应生成边界输入节点");
        helper.assertTrue("c2_3".equals(down.get("b2_2").nextId()),
                "边界节点应挂在最近的芯片节点上，实际 " + down.get("b2_2").nextId());

        Map<String, FactoryTreeModel.Node> up = index(upstream);
        helper.assertTrue(up.keySet().containsAll(List.of("out", "c2_1", "in2")),
                "上游步骤节点集合不符合预期: " + up.keySet());
        helper.assertTrue("c2_1".equals(up.get("in2").nextId()),
                "上游输入叶子应挂在芯片节点上，实际 " + up.get("in2").nextId());

        // 芯片列表：邻格芯片算在本通路格上；连接芯片只作用于自身所在格（effectSelf），不会串到邻格
        helper.assertTrue(FactoryTreeModel.chipsOf(be, 2, 4).size() == 1, "通路格 (2,4) 应有 1 颗邻格芯片");
        helper.assertTrue(FactoryTreeModel.chipsOf(be, 2, 3).size() == 1,
                "通路格 (2,3) 只应受邻格芯片影响，不应额外收集到连接芯片");
        List<FactoryTreeModel.ChipRef> concatChips = FactoryTreeModel.chipsOf(be, 2, 2);
        helper.assertTrue(concatChips.size() == 1 && concatChips.getFirst().row() == 2 && concatChips.getFirst().col() == 2,
                "连接芯片应挂在通路格 (2,2) 自身上，实际 " + concatChips);

        // 没有连接芯片时不切分
        be.internalContainer.setItem(2 * cols + 2, chipStack("stamping_chip"));
        helper.assertTrue(FactoryTreeModel.build(be).groups().size() == 1,
                "去掉连接芯片后应只剩 1 个配方");

        helper.succeed();
    }

    // ------------------------------------------------------------------ 辅助

    private static EtherProcessFactoryEntity newFactory() {
        return newFactory(1);
    }

    private static EtherProcessFactoryEntity newFactory(int level) {
        return new EtherProcessFactoryEntity(BlockPos.ZERO,
                BlockRegistry.ETHER_PROCESS_FACTORY.get().defaultBlockState()
                        .setValue(EtherProcessFactoryBlock.LEVEL, level));
    }

    private static ItemStack chipStack(String chipId) {
        return ProcessChipItem.getStackFor(EtherCraft.id(chipId));
    }

    private static void clearPath(EtherProcessFactoryEntity be) {
        for (int row = 0; row < be.ROWS; row++) {
            be.pathMaxDepth[row] = 0;
            for (int col = 0; col < be.COLS; col++)
                be.pathBelongings[row][col] = -1;
        }
    }

    private static void setPath(EtherProcessFactoryEntity be, int row, int col,
                                int outputIndex, int depth, int direction) {
        be.pathBelongings[row][col] = outputIndex;
        be.pathDepth[row][col] = depth;
        be.pathDirection[row][col] = direction;
    }

    private static Map<String, FactoryTreeModel.Node> index(FactoryTreeModel.Group group) {
        Map<String, FactoryTreeModel.Node> nodes = new HashMap<>();
        for (FactoryTreeModel.Node node : group.nodes()) nodes.put(node.id(), node);
        return nodes;
    }

    private static TreeDiagramSpec spec(TreeDiagramSpec.NodeSpec... nodes) {
        return new TreeDiagramSpec(List.of(nodes), new TreeDiagramSpec.OutputSpec(0),
                new TreeDiagramSpec.LayoutConfig(18, 18, 1, 0, 140, 0, 4, 8, 26));
    }
}
