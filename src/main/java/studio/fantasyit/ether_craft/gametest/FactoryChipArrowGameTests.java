package studio.fantasyit.ether_craft.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.factory.EtherProcessWorkingChip;
import studio.fantasyit.ether_craft.item.ProcessChipItem;
import studio.fantasyit.ether_craft.menu.factory.chip.ChipChannelArrows;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessorRecipeUtil;
import studio.fantasyit.ether_craft.recipe.factory.PathNode;
import studio.fantasyit.ether_craft.recipe.factory.multistep.EtherFactoryMultiStepInput;

/**
 * 通道箭头判定测试（{@link ChipChannelArrows}）。
 *
 * <p>复用连接芯片的 7x7 直走廊布局，按 {@code EtherProcessFactoryEntity} 相同的方式推导出
 * belonging / pathDepth 数组，再断言四个方向：沿流动方向（左 -&gt; 右）编号更大的邻居为「出」、
 * 编号更小的为「入」，越界或不在通道上的方向一律「封闭」。
 */
public final class FactoryChipArrowGameTests {
    private static final int ROWS = 7;
    private static final int COLS = 7;
    private static final int CORRIDOR_ROW = 3;

    private FactoryChipArrowGameTests() {
    }

    public static void channelArrowsFixture(GameTestHelper helper) {
        EtherProcessWorkingChip[][] chips = new EtherProcessWorkingChip[ROWS][COLS];
        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS; col++)
                chips[row][col] = workingChip("separator_chip");

        for (int col = 0; col < COLS; col++)
            chips[CORRIDOR_ROW][col] = null;
        chips[CORRIDOR_ROW][2] = workingChip("concat_chip");
        // 走廊上下各挂一颗冲压，保证通路能建成树
        for (int col : new int[]{1, 3, 4, 5}) {
            chips[CORRIDOR_ROW - 1][col] = workingChip("stamping_chip");
            chips[CORRIDOR_ROW + 1][col] = workingChip("separator_chip");
        }

        SimpleContainer input = new SimpleContainer(ROWS);
        input.setItem(CORRIDOR_ROW, new ItemStack(Items.COBBLESTONE, 64));

        EtherProcessorRecipeUtil.FactoryStructure structure =
                EtherProcessorRecipeUtil.processFactoryInput(ROWS, COLS, input, chips);
        helper.assertTrue(structure.recipes.size() == 1,
                "直走廊布局应产生 1 个候选通道，实际 " + structure.recipes.size());

        int[][] belonging = new int[ROWS][COLS];
        int[][] pathDepth = new int[ROWS][COLS];
        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS; col++)
                belonging[row][col] = -1;

        for (EtherFactoryMultiStepInput candidate : structure.recipes) {
            int channel = candidate.outputI();
            int maxDepth = candidate.maxDepth();
            for (PathNode node : candidate.workingPath()) {
                belonging[node.pos().y][node.pos().x] = channel;
                pathDepth[node.pos().y][node.pos().x] = maxDepth - node.depth();
            }
        }

        // 走廊整行成型，且编号沿前进方向（左 -> 右）递增
        for (int col = 0; col < COLS; col++) {
            helper.assertTrue(belonging[CORRIDOR_ROW][col] >= 0,
                    "走廊第 " + col + " 格应在通道上");
            if (col > 0)
                helper.assertTrue(pathDepth[CORRIDOR_ROW][col] > pathDepth[CORRIDOR_ROW][col - 1],
                        "通道编号应沿前进方向递增，第 " + col + " 格异常");
        }

        // 中段：左入右出，上下封闭
        assertEdges(helper, belonging, pathDepth, CORRIDOR_ROW, 3, "走廊中段",
                ChipChannelArrows.ArrowKind.CLOSED, ChipChannelArrows.ArrowKind.CLOSED,
                ChipChannelArrows.ArrowKind.IN, ChipChannelArrows.ArrowKind.OUT);

        // 输入端（最左格）：左侧越界封闭，右侧为出
        assertEdges(helper, belonging, pathDepth, CORRIDOR_ROW, 0, "走廊最左格",
                ChipChannelArrows.ArrowKind.CLOSED, ChipChannelArrows.ArrowKind.CLOSED,
                ChipChannelArrows.ArrowKind.CLOSED, ChipChannelArrows.ArrowKind.OUT);

        // 输出端（最右格）：左侧为入，右侧越界封闭
        assertEdges(helper, belonging, pathDepth, CORRIDOR_ROW, COLS - 1, "走廊最右格",
                ChipChannelArrows.ArrowKind.CLOSED, ChipChannelArrows.ArrowKind.CLOSED,
                ChipChannelArrows.ArrowKind.IN, ChipChannelArrows.ArrowKind.CLOSED);

        // 不在通道上的格子：四边全封闭
        assertEdges(helper, belonging, pathDepth, 0, 0, "通道外的格子",
                ChipChannelArrows.ArrowKind.CLOSED, ChipChannelArrows.ArrowKind.CLOSED,
                ChipChannelArrows.ArrowKind.CLOSED, ChipChannelArrows.ArrowKind.CLOSED);

        // 通道完全不成型：四边全封闭
        int[][] none = new int[ROWS][COLS];
        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS; col++)
                none[row][col] = -1;
        assertEdges(helper, none, none, CORRIDOR_ROW, 3, "不成型通道",
                ChipChannelArrows.ArrowKind.CLOSED, ChipChannelArrows.ArrowKind.CLOSED,
                ChipChannelArrows.ArrowKind.CLOSED, ChipChannelArrows.ArrowKind.CLOSED);

        helper.succeed();
    }

    private static void assertEdges(GameTestHelper helper, int[][] belonging, int[][] pathDepth,
                                    int row, int col, String where,
                                    ChipChannelArrows.ArrowKind up, ChipChannelArrows.ArrowKind down,
                                    ChipChannelArrows.ArrowKind left, ChipChannelArrows.ArrowKind right) {
        ChipChannelArrows.ArrowKind[] kinds = ChipChannelArrows.edges(belonging, pathDepth, row, col);
        helper.assertTrue(kinds[PathNode.Direction.UP.ordinal()] == up,
                where + " 上箭头应为 " + up + "，实际 " + kinds[PathNode.Direction.UP.ordinal()]);
        helper.assertTrue(kinds[PathNode.Direction.DOWN.ordinal()] == down,
                where + " 下箭头应为 " + down + "，实际 " + kinds[PathNode.Direction.DOWN.ordinal()]);
        helper.assertTrue(kinds[PathNode.Direction.LEFT.ordinal()] == left,
                where + " 左箭头应为 " + left + "，实际 " + kinds[PathNode.Direction.LEFT.ordinal()]);
        helper.assertTrue(kinds[PathNode.Direction.RIGHT.ordinal()] == right,
                where + " 右箭头应为 " + right + "，实际 " + kinds[PathNode.Direction.RIGHT.ordinal()]);
    }

    private static EtherProcessWorkingChip workingChip(String chipId) {
        return new EtherProcessWorkingChip(ProcessChipItem.getStackFor(EtherCraft.id(chipId)));
    }
}
