package studio.fantasyit.ether_craft.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.factory.EtherProcessWorkingChip;
import studio.fantasyit.ether_craft.item.ProcessChipItem;
import studio.fantasyit.ether_craft.recipe.factory.EtherFactoryRecipeInput;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeManager;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessorRecipeUtil;
import studio.fantasyit.ether_craft.recipe.factory.RecipeNode;
import studio.fantasyit.ether_craft.recipe.factory.multistep.EtherFactoryMultiStepInput;
import studio.fantasyit.ether_craft.recipe.factory.multistep.MultiStepMatchIO;
import studio.fantasyit.ether_craft.recipe.factory.multistep.TreeRef;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 连接（concat）芯片的分步配方识别测试。
 *
 * <p>布局（7×7，corridor 在第 3 行）：圆石 --[冲压]--> 连接芯片 --[冲压]-->[冲压]-->[冲压]--> 沙子，
 * 对应数据包里的 {@code minecraft_gravel}（圆石→沙砾，1 步）与 {@code minecraft_sand}（沙砾→沙子，3 步）。
 */
public final class FactoryConcatFlowGameTests {
    private FactoryConcatFlowGameTests() {
    }

    private static final int ROWS = 7;
    private static final int COLS = 7;
    private static final int CORRIDOR_ROW = 3;
    private static final int OUTPUT_ROW = 3;

    public static void concatFlowFixture(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        EtherProcessWorkingChip[][] chips = new EtherProcessWorkingChip[ROWS][COLS];
        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS; col++)
                chips[row][col] = workingChip("separator_chip");

        for (int col = 0; col < COLS; col++)
            chips[CORRIDOR_ROW][col] = null;
        chips[CORRIDOR_ROW][2] = workingChip("concat_chip");

        // 每格通道的上下各有一个"墙"，芯片则挂在该格上
        putProcessChip(chips, 1, "stamping_chip");   // 连接芯片上游：圆石 -> 沙砾（1 步）
        putProcessChip(chips, 3, "stamping_chip");   // 连接芯片下游：沙砾 -> 沙子（3 步）
        putProcessChip(chips, 4, "stamping_chip");
        putProcessChip(chips, 5, "stamping_chip");

        SimpleContainer input = new SimpleContainer(ROWS);
        input.setItem(OUTPUT_ROW, new ItemStack(Items.COBBLESTONE, 64));

        EtherProcessorRecipeUtil.FactoryStructure structure =
                EtherProcessorRecipeUtil.processFactoryInput(ROWS, COLS, input, chips);
        helper.assertTrue(structure.recipes.size() == 1,
                "该连接布局应产生 1 个候选树，实际 " + structure.recipes.size());
        EtherFactoryMultiStepInput candidate = structure.recipes.getFirst();
        helper.assertTrue(candidate.outputI() == OUTPUT_ROW,
                "候选树应对应输出行 " + OUTPUT_ROW + "，实际 " + candidate.outputI());

        Optional<MultiStepMatchIO> matched = EtherProcessRecipeManager.getRecipe(level, level.recipeAccess(), candidate);
        helper.assertTrue(matched.isPresent(), "连接布局应能匹配到配方");
        MultiStepMatchIO io = matched.get();
        helper.assertTrue(io.success(), "连接布局的配方匹配应成功");
        List<ItemStack> outputs = io.outputs();
        helper.assertTrue(!outputs.isEmpty() && outputs.getFirst().is(Items.SAND),
                "连接布局的最终产物应为沙子，实际 " + outputs.stream().map(s -> s.getItem().toString()).toList());

        helper.succeed();
    }

/**
     * 复刻指南 factory_10.png 的示例：三条走廊共用墙壁芯片。
     *  - 走廊 1（第 1 行）：(0,1) 一颗冲压 -> 圆石合成沙砾
     *  - 走廊 3（第 3 行）：(4,3)(4,4)(4,5) 三颗冲压 -> 沙砾合成沙子
     *  - 走廊 5（第 5 行）：(5,2) 连接芯片 + (6,1) 一颗冲压 + (4,3..5) 三颗冲压 -> 圆石直接合成沙子
     */
    public static void concatGuideLayoutFixture(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        EtherProcessWorkingChip[][] chips = new EtherProcessWorkingChip[ROWS][COLS];
        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS; col++)
                chips[row][col] = workingChip("separator_chip");
        for (int col = 0; col < COLS; col++) {
            chips[1][col] = null;
            chips[3][col] = null;
            chips[5][col] = null;
        }
        chips[5][2] = workingChip("concat_chip");
        chips[0][1] = workingChip("stamping_chip");
        chips[4][3] = workingChip("stamping_chip");
        chips[4][4] = workingChip("stamping_chip");
        chips[4][5] = workingChip("stamping_chip");
        chips[6][1] = workingChip("stamping_chip");

        SimpleContainer input = new SimpleContainer(ROWS);
        input.setItem(1, new ItemStack(Items.COBBLESTONE, 64));
        input.setItem(3, new ItemStack(Items.GRAVEL, 64));
        input.setItem(5, new ItemStack(Items.COBBLESTONE, 64));

        EtherProcessorRecipeUtil.FactoryStructure structure =
                EtherProcessorRecipeUtil.processFactoryInput(ROWS, COLS, input, chips);
        helper.assertTrue(structure.recipes.size() == 3,
                "三条走廊都应被识别，实际 " + structure.recipes.size() + " 个候选");

        for (EtherFactoryMultiStepInput candidate : structure.recipes) {
            Optional<MultiStepMatchIO> matched = EtherProcessRecipeManager.getRecipe(level, level.recipeAccess(), candidate);
            helper.assertTrue(matched.isPresent() && matched.get().success(),
                    "输出行 " + candidate.outputI() + " 应能匹配到配方");
            List<ItemStack> outputs = matched.get().outputs();
            String expected = candidate.outputI() == 1 ? "gravel" : "sand";
            helper.assertTrue(!outputs.isEmpty() && outputs.getFirst().is(
                            candidate.outputI() == 1 ? Items.GRAVEL : Items.SAND),
                    "输出行 " + candidate.outputI() + " 的产物应为 " + expected + "，实际 "
                            + outputs.stream().map(s -> s.getItem().toString()).toList());
        }

        helper.succeed();
    }


    // ------------------------------------------------------------------ 辅助

    private static void putProcessChip(EtherProcessWorkingChip[][] chips, int col, String chipId) {
        chips[CORRIDOR_ROW - 1][col] = workingChip(chipId);
        chips[CORRIDOR_ROW + 1][col] = workingChip("separator_chip");
    }

    private static EtherProcessWorkingChip workingChip(String chipId) {
        return new EtherProcessWorkingChip(ProcessChipItem.getStackFor(EtherCraft.id(chipId)));
    }

    private static String dump(EtherFactoryMultiStepInput input) {
        StringBuilder sb = new StringBuilder();
        sb.append("outputI=").append(input.outputI())
                .append(" maxDepth=").append(input.maxDepth())
                .append(" inputIds=").append(input.inputIds())
                .append(" inputs=").append(input.inputs().stream()
                        .map(s -> s.getItem() + "x" + s.getCount()).toList())
                .append(" outputPositions=").append(input.outputPositions());
        sb.append("\n  refTree:");
        dumpRef(input.processInputTrees().getRoot(), 2, sb, new HashSet<>());
        return sb.toString();
    }

    private static void dumpRef(TreeLike.TreeNode<TreeRef, Integer> node, int depth, StringBuilder sb,
                                Set<Integer> visited) {
        if (node == null || !visited.add(node.id)) return;
        sb.append("\n").append("  ".repeat(depth)).append("ref#").append(node.id);
        if (node.value != null) {
            sb.append(" output=").append(node.value.output())
                    .append(" inputMapping=").append(node.value.inputMapping());
            sb.append("\n").append("  ".repeat(depth + 1)).append("inner:");
            dumpRecipe(node.value.tree().getRoot(), depth + 2, sb, new HashSet<>());
        }
        for (TreeLike.TreeEdge<TreeRef, Integer> edge : node.edges) dumpRef(edge.node, depth + 1, sb, visited);
    }

    private static void dumpRecipe(TreeLike.TreeNode<List<Integer>, RecipeNode> node, int depth,
                                   StringBuilder sb, Set<Integer> visited) {
        if (node == null || !visited.add(node.id)) return;
        sb.append("\n").append("  ".repeat(depth)).append("n").append(node.id);
        for (TreeLike.TreeEdge<List<Integer>, RecipeNode> edge : node.edges) {
            sb.append("\n").append("  ".repeat(depth + 1))
                    .append("(").append(edge.value.x()).append(",").append(edge.value.y()).append(") chips=");
            for (ItemStack chip : edge.value.input()) {
                sb.append(chip.get(DataComponentRegistry.CHIP_ID)).append(" ");
            }
            dumpRecipe(edge.node, depth + 2, sb, visited);
        }
    }
}
