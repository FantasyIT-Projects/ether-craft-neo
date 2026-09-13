package studio.fantasyit.ether_craft.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.recipe.DelayedIngredient;
import studio.fantasyit.ether_craft.recipe.IngredientSerializer;
import studio.fantasyit.ether_craft.recipe.factory.EtherFactoryRecipeInput;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeJson;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeManager;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessorRecipeUtil;
import studio.fantasyit.ether_craft.recipe.factory.RecipeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 加工中心（{@code ether_craft:ether_process}）配方匹配的 GameTest 单元测试。
 *
 * <p>匹配入口为 {@link EtherProcessFactoryRecipe#matches}，其核心是
 * {@link EtherProcessorRecipeUtil#isRecipeCompatible}：先把工厂芯片布局化成输入树，
 * 再与配方处理树做“同构 + 边芯片全排列匹配 + 输入物品二分图完美匹配”。
 *
 * <p>本测试不放置任何方块，直接构造输入树：由配方自身推出一棵镜像输入树，
 * 它必然匹配成功；随后对配方/输入做定向破坏，验证必须匹配失败。
 */
public final class FactoryRecipeMatchGameTests {
    private FactoryRecipeMatchGameTests() {
    }

    // ------------------------------------------------------------------
    // 测试 1：固定夹具正例 + 定向破坏反例
    // ------------------------------------------------------------------
    public static void recipeMatchFixture(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        EtherProcessFactoryRecipe single = fixtureSingleStep(List.of(chip("welding_chip")));
        EtherProcessFactoryRecipe chain = fixtureChain();
        EtherProcessFactoryRecipe merge = fixtureMerge(List.of(chip("stamping_chip"), chip("cutting_chip")));
        EtherProcessFactoryRecipe mergeReversed = fixtureMerge(List.of(chip("cutting_chip"), chip("stamping_chip")));
        EtherProcessFactoryRecipe wrongChip = fixtureSingleStep(List.of(chip("carving_chip")));

        // --- 正例：配方与自身镜像输入树必须匹配 ---
        expectMatch(helper, level, single, "单步配方应能与自身镜像输入树匹配");
        expectMatch(helper, level, chain, "两步链配方应能与自身镜像输入树匹配");
        expectMatch(helper, level, merge, "双输入汇合配方应能与自身镜像输入树匹配");

        // --- 正例：边芯片书写顺序无关（全排列匹配语义） ---
        helper.assertTrue(merge.matches(RecipeMirrorBuilder.mirrorOf(mergeReversed), level),
                "芯片顺序不同的等价配方，其镜像输入树应仍能匹配");

        // --- 反例 1：芯片种类不符 ---
        helper.assertFalse(single.matches(RecipeMirrorBuilder.mirrorOf(wrongChip), level),
                "芯片种类不符时不应匹配成功");

        // --- 反例 2：输入物品不符 ---
        EtherFactoryRecipeInput wrongItem = RecipeMirrorBuilder.mirrorOf(chain);
        wrongItem.inputs.set(0, new ItemStack(Items.DIAMOND, 2));
        expectNotMatch(helper, level, chain, wrongItem, "输入物品与任何原料都不符时不应匹配成功");

        // --- 反例 3：输入数量不足 ---
        EtherFactoryRecipeInput lowCount = RecipeMirrorBuilder.mirrorOf(chain);
        lowCount.inputs.set(0, lowCount.inputs.get(0).copyWithCount(1));
        expectNotMatch(helper, level, chain, lowCount, "输入数量低于原料需求时不应匹配成功");

        // --- 反例 4：输入项数少于配方（去掉一个输入） ---
        EtherFactoryRecipeInput fewerInputs = RecipeMirrorBuilder.mirrorOf(merge);
        fewerInputs.inputs.remove(fewerInputs.inputs.size() - 1);
        fewerInputs.inputTreeIds.remove(fewerInputs.inputTreeIds.size() - 1);
        expectNotMatch(helper, level, merge, fewerInputs, "输入项数少于配方输入项数时不应匹配成功");

        // --- 反例 5：输入项数多于配方（多塞一个输入） ---
        EtherFactoryRecipeInput moreInputs = RecipeMirrorBuilder.mirrorOf(merge);
        moreInputs.inputs.add(new ItemStack(Items.IRON_INGOT, 1));
        moreInputs.inputTreeIds.add(9999);
        expectNotMatch(helper, level, merge, moreInputs, "输入项数多于配方输入项数时不应匹配成功");

        // --- 反例 6：输入树上某条边的芯片数量不符 ---
        EtherFactoryRecipeInput fewerChips = RecipeMirrorBuilder.mirrorOf(merge);
        helper.assertTrue(removeOneChipFromMultiChipEdge(fewerChips.process),
                "夹具镜像输入树中应存在多芯片边，否则测试夹具失效");
        expectNotMatch(helper, level, merge, fewerChips, "某条边的芯片数量与配方不符时不应匹配成功");

        // --- 反例 7：输入树结构不符（删掉一层出边） ---
        EtherFactoryRecipeInput brokenTree = RecipeMirrorBuilder.mirrorOf(chain);
        TreeLike.TreeNode<List<Integer>, RecipeNode> root = brokenTree.process.getRoot();
        helper.assertTrue(!root.edges.isEmpty(), "夹具镜像输入树根节点应存在出边，否则测试夹具失效");
        root.edges.get(0).node.edges.clear();
        expectNotMatch(helper, level, chain, brokenTree, "输入树层数与配方不符时不应匹配成功");

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 测试 2：数据包内全部 ether_process 配方自匹配
    // ------------------------------------------------------------------
    public static void recipeMatchDatapack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager manager = Objects.requireNonNull(level.getServer(), "GameTest 中 server 不应为空")
                .getRecipeManager();

        List<Map.Entry<Identifier, EtherProcessFactoryRecipe>> recipes = new ArrayList<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (holder.value() instanceof EtherProcessFactoryRecipe recipe) {
                recipes.add(Map.entry(holder.id().identifier(), recipe));
            }
        }
        helper.assertTrue(!recipes.isEmpty(), "数据包中应至少存在一条 ether_craft:ether_process 配方");

        Identifier logoId = EtherCraft.id("ether_process/logo");
        EtherProcessFactoryRecipe logo = null;
        List<String> failures = new ArrayList<>();
        for (Map.Entry<Identifier, EtherProcessFactoryRecipe> entry : recipes) {
            if (entry.getKey().equals(logoId)) {
                logo = entry.getValue();
            }
            EtherFactoryRecipeInput input = RecipeMirrorBuilder.mirrorOf(entry.getValue());
            if (RecipeMirrorBuilder.hasCycle(input.process)) {
                failures.add(entry.getKey() + "(配方树含环)");
                continue;
            }
            if (!entry.getValue().matches(input, level)) {
                failures.add(entry.getKey().toString());
            }
        }
        EtherCraft.LOGGER.info("[gametest] ether_process 配方自匹配: {}/{} 通过",
                recipes.size() - failures.size(), recipes.size());
        helper.assertTrue(failures.isEmpty(),
                "以下 ether_process 配方无法与自身镜像输入树匹配（" + failures.size() + "/" + recipes.size() + "）: "
                        + failures);
        helper.assertTrue(logo != null, "数据包中应存在配方 " + logoId);

        // 覆盖工厂实际使用的注册表查询路径
        Optional<EtherProcessFactoryRecipe> found =
                EtherProcessRecipeManager.getRecipe(level, manager, RecipeMirrorBuilder.mirrorOf(logo));
        helper.assertTrue(found.isPresent(), "EtherProcessRecipeManager#getRecipe 应能为 logo 配方的镜像输入找到配方");

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 测试 3：输入 ↔ 原料 的计数映射
    // ------------------------------------------------------------------
    public static void recipeCostMapping(GameTestHelper helper) {
        int[] mapping = EtherProcessorRecipeUtil.getToCostCountByInputAndIngredient(
                List.of(new ItemStack(Items.IRON_INGOT, 2), new ItemStack(Items.GOLD_INGOT, 1)),
                List.of(SizedIngredient.of(Items.GOLD_INGOT, 1), SizedIngredient.of(Items.IRON_INGOT, 2)));
        helper.assertTrue(mapping.length == 2, "映射结果长度应与输入项数一致，实际为 " + mapping.length);
        helper.assertTrue(mapping[0] == 1 && mapping[1] == 0,
                "输入[铁x2, 金x1] 应映射到 原料[金x1, 铁x2]，实际为 " + Arrays.toString(mapping));

        int[] insufficient = EtherProcessorRecipeUtil.getToCostCountByInputAndIngredient(
                List.of(new ItemStack(Items.IRON_INGOT, 1)),
                List.of(SizedIngredient.of(Items.IRON_INGOT, 2)));
        helper.assertTrue(insufficient.length == 1 && insufficient[0] == -1,
                "输入数量不足时映射结果应为 -1，实际为 " + Arrays.toString(insufficient));

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 断言辅助
    // ------------------------------------------------------------------
    private static void expectMatch(GameTestHelper helper, ServerLevel level,
                                    EtherProcessFactoryRecipe recipe, String message) {
        EtherFactoryRecipeInput input = RecipeMirrorBuilder.mirrorOf(recipe);
        if (RecipeMirrorBuilder.hasCycle(input.process)) {
            helper.fail(message + "：镜像输入树含环，为避免匹配算法死循环而中止");
            return;
        }
        helper.assertTrue(recipe.matches(input, level), message);
    }

    private static void expectNotMatch(GameTestHelper helper, ServerLevel level,
                                       EtherProcessFactoryRecipe recipe, EtherFactoryRecipeInput input,
                                       String message) {
        helper.assertFalse(recipe.matches(input, level), message);
    }

    /** 在输入树中移除任意一条多芯片边上的一个芯片，返回是否移除成功。 */
    private static boolean removeOneChipFromMultiChipEdge(TreeLike<List<Integer>, RecipeNode> tree) {
        for (TreeLike.TreeNode<List<Integer>, RecipeNode> node : tree.getNodes()) {
            for (TreeLike.TreeEdge<List<Integer>, RecipeNode> edge : node.edges) {
                if (edge.value.input().size() > 1) {
                    edge.value.input().remove(0);
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // 夹具配方
    // ------------------------------------------------------------------
    /** I0(铁x1) -> P0(指定芯片)。 */
    private static EtherProcessFactoryRecipe fixtureSingleStep(List<DelayedIngredient> p0Chips) {
        return recipe(
                List.of(input("I0", Items.IRON_INGOT, 1, "P0")),
                List.of(process("P0", p0Chips, null)));
    }

    /** I0(金x2) -> P1(molding) -> P0(welding)。 */
    private static EtherProcessFactoryRecipe fixtureChain() {
        return recipe(
                List.of(input("I0", Items.GOLD_INGOT, 2, "P1")),
                List.of(process("P1", List.of(chip("molding_chip")), "P0"),
                        process("P0", List.of(chip("welding_chip")), null)));
    }

    /** I0(铁x1)、I1(铜x1) -> P1(两枚芯片) -> P0(welding)。 */
    private static EtherProcessFactoryRecipe fixtureMerge(List<DelayedIngredient> p1Chips) {
        return recipe(
                List.of(input("I0", Items.IRON_INGOT, 1, "P1"), input("I1", Items.COPPER_INGOT, 1, "P1")),
                List.of(process("P1", p1Chips, "P0"),
                        process("P0", List.of(chip("welding_chip")), null)));
    }

    /** 芯片原料使用与数据包一致的严格组件匹配（{@code ChipRecord}）。 */
    private static DelayedIngredient chip(String path) {
        return DelayedIngredient.of(new IngredientSerializer.ChipRecord(EtherCraft.id(path)));
    }

    private static EtherProcessRecipeJson.InputEntry input(String id, Item item, int count, String next) {
        return new EtherProcessRecipeJson.InputEntry(id, SizedIngredient.of(item, count), next);
    }

    private static EtherProcessRecipeJson.ProcessEntry process(String id, List<DelayedIngredient> chips, String next) {
        return new EtherProcessRecipeJson.ProcessEntry(id, chips, next);
    }

    private static EtherProcessFactoryRecipe recipe(List<EtherProcessRecipeJson.InputEntry> inputs,
                                                    List<EtherProcessRecipeJson.ProcessEntry> process) {
        return new EtherProcessFactoryRecipe(new EtherProcessRecipeJson(
                inputs, new EtherProcessRecipeJson.OutputEntry("O", List.of()), process));
    }
}
