package studio.fantasyit.ether_craft.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.factory.EtherProcessChipManager;
import studio.fantasyit.ether_craft.factory.EtherProcessWorkingChip;
import studio.fantasyit.ether_craft.factory.special.SpecialRecipeManager;
import studio.fantasyit.ether_craft.factory.special.SpecialRecipeMatchResult;
import studio.fantasyit.ether_craft.item.CraftTableChipItem;
import studio.fantasyit.ether_craft.item.ProcessChipItem;
import studio.fantasyit.ether_craft.recipe.factory.EtherFactoryRecipeInput;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeManager;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessorRecipeUtil;
import studio.fantasyit.ether_craft.recipe.factory.multistep.EtherFactoryMultiStepInput;
import studio.fantasyit.ether_craft.recipe.factory.multistep.MultiStepMatchIO;
import studio.fantasyit.ether_craft.recipe.factory.multistep.TreeRef;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 工作台芯片 / 特殊配方预匹配测试。
 *
 * <p>布局（7×7，通路在第 3 行）：工作台芯片放在<b>通路格本身</b>上
 * （默认元：separate=false / effectSelf=true / effectSide=false）。
 * 需要多输入时在通路上挂竖直支路：第 5 行（连接格 (3,4)）、第 1 行（连接格 (1,2)），
 * 分别从第 5 / 第 1 行的输入口供料。
 */
public final class SpecialRecipeGameTests {
    private SpecialRecipeGameTests() {
    }

    private static final int ROWS = 7;
    private static final int COLS = 7;
    private static final int CORRIDOR_ROW = 3;
    private static final int LOW_BRANCH_ROW = 5;
    private static final int HIGH_BRANCH_ROW = 1;
    private static final int CHIP_COL = 3;

    /** 无序配方（分支 1）：一次 4 个橡木木板。 */
    private static final Identifier SHAPELESS_RECIPE = Identifier.withDefaultNamespace("oak_planks");
    /** 有形状配方（分支 1）：2 格图案（煤炭在上、木棍在下），一次 4 个火把。 */
    private static final Identifier SHAPED_RECIPE = Identifier.withDefaultNamespace("torch");
    /** fallback 网格无关匹配（分支 2 主路径）：纸 + 火药 → 3 个烟花火箭。 */
    private static final Identifier FALLBACK_ROW_RECIPE = Identifier.withDefaultNamespace("firework_rocket");
    /** fallback 排布兜底（分支 2 兜底）：模组自己的 upgrade_shaped 3×3 配方（id 含 recipe 目录名）。 */
    private static final Identifier FALLBACK_SHAPED_RECIPE = EtherCraft.id("crafting/ether_process_factory_lv_2");

    // ------------------------------------------------------------------ 用例

    /** 普通配方树里没有工作台芯片 → NotSpecial（走常规匹配）。 */
    public static void craftTableNotSpecial(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager manager = level.recipeAccess();

        EtherProcessFactoryRecipe logo = findRecipe(manager, "ether_process/logo");
        helper.assertTrue(logo != null, "数据包中应存在 ether_craft:ether_process/logo 配方");

        EtherFactoryRecipeInput mirror = RecipeMirrorBuilder.mirrorOf(logo);
        SpecialRecipeMatchResult result = SpecialRecipeManager.match(level, manager, mirror.process, mirror);
        helper.assertTrue(result instanceof SpecialRecipeMatchResult.NotSpecial,
                "不含工作台芯片的普通配方树应为 NotSpecial，实际 " + result);
        helper.succeed();
    }

    /** 无序配方（分支 1）：原料列表决定格位，产物数量取自 assemble。 */
    public static void craftTableMatched(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager manager = level.recipeAccess();

        CraftingRecipe target = findCraftingRecipe(manager, SHAPELESS_RECIPE);
        helper.assertTrue(target != null, "应存在无序合成配方 " + SHAPELESS_RECIPE);

        ItemStack inputStack = sampleFirstIngredient(target);
        ItemStack chipStack = CraftTableChipItem.getStack(SHAPELESS_RECIPE, List.of());

        helper.assertTrue(EtherProcessChipManager.get(chipStack) != null,
                "工作台芯片应有对应的数据包芯片记录（ether_process_chip/craft_table_chip.json）");
        helper.assertTrue(EtherProcessChipManager.get(chipStack).effect().effectSelf()
                        && !EtherProcessChipManager.get(chipStack).effect().separate()
                        && !EtherProcessChipManager.get(chipStack).effect().effectSide(),
                "工作台芯片默认元应为：影响当前格子、不阻挡通路、不影响周围格子");

        EtherFactoryMultiStepInput candidate = layout(helper, chipStack, List.of(inputStack));
        EtherFactoryRecipeInput refInput = refInput(candidate);

        SpecialRecipeMatchResult direct = SpecialRecipeManager.match(level, manager, refInput.process, refInput);
        helper.assertTrue(direct instanceof SpecialRecipeMatchResult.Matched,
                "含工作台芯片的树应匹配到特殊配方，实际 " + direct);

        // 产物数量 4 证明走的是 assemble（按 SlotDisplay 解析只会得到 1 个）
        expectOutput(helper, level, manager, candidate, Items.OAK_PLANKS, 4, "无序配方");
        helper.succeed();
    }

    /** 有形状配方（分支 1）：图案格位 + 二分图匹配，输入顺序与图案顺序相反也应成功。 */
    public static void craftTableShaped(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager manager = level.recipeAccess();

        CraftingRecipe target = findCraftingRecipe(manager, SHAPED_RECIPE);
        helper.assertTrue(target != null, "应存在有形状合成配方 " + SHAPED_RECIPE);

        ItemStack chipStack = CraftTableChipItem.getStack(SHAPED_RECIPE, List.of());
        // 火把图案是第一格煤炭、第二格木棍；这里故意反着喂，验证走二分图匹配而不是按序填格
        EtherFactoryMultiStepInput candidate = layout(helper, chipStack,
                List.of(new ItemStack(Items.STICK), new ItemStack(Items.COAL)));
        EtherFactoryRecipeInput refInput = refInput(candidate);

        SpecialRecipeMatchResult result = SpecialRecipeManager.match(level, manager, refInput.process, refInput);
        helper.assertTrue(result instanceof SpecialRecipeMatchResult.Matched,
                "有形状配方应匹配成功，实际 " + result);

        expectOutput(helper, level, manager, candidate, Items.TORCH, 4, "有形状配方");
        helper.succeed();
    }

    /** 未配置 / 配方 id 无效 / 输入不满足原料 → NoMatch（严格失败，不回退常规匹配）。 */
    public static void craftTableNoMatch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager manager = level.recipeAccess();

        CraftingRecipe target = findCraftingRecipe(manager, SHAPELESS_RECIPE);
        helper.assertTrue(target != null, "应存在无序合成配方 " + SHAPELESS_RECIPE);
        ItemStack inputStack = sampleFirstIngredient(target);

        // 1) 完全没有数据组件的工作台芯片
        ItemStack unset = ProcessChipItem.getStackFor(CraftTableChipItem.CHIP_ID);
        expectNoMatch(helper, level, manager, unset, List.of(inputStack), "未配置的工作台芯片");

        // 2) 数据组件指向不存在的配方
        ItemStack bogus = CraftTableChipItem.getStack(
                Identifier.fromNamespaceAndPath("ether_craft", "not_a_recipe"), List.of());
        expectNoMatch(helper, level, manager, bogus, List.of(inputStack), "配方 id 无效的工作台芯片");

        // 3) 输入物品不满足配方原料（橡木木板配方只接受 #minecraft:oak_logs）
        ItemStack wrongInputChip = CraftTableChipItem.getStack(SHAPELESS_RECIPE, List.of());
        expectNoMatch(helper, level, manager, wrongInputChip,
                List.of(new ItemStack(Items.DIAMOND)), "输入与原料不符的工作台芯片");

        helper.succeed();
    }

    /** fallback 主路径（分支 2）：芯片预存列表与实际输入网格无关匹配，再按输入格子装单行网格。 */
    public static void craftTableFallbackRow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager manager = level.recipeAccess();

        helper.assertTrue(findCraftingRecipe(manager, FALLBACK_ROW_RECIPE) != null,
                "应存在 " + FALLBACK_ROW_RECIPE);

        ItemStack chipStack = CraftTableChipItem.getStack(FALLBACK_ROW_RECIPE, List.of(
                new ItemStack(Items.PAPER),
                new ItemStack(Items.GUNPOWDER)));
        EtherFactoryMultiStepInput candidate = layout(helper, chipStack,
                List.of(new ItemStack(Items.PAPER), new ItemStack(Items.GUNPOWDER)));
        EtherFactoryRecipeInput refInput = refInput(candidate);

        SpecialRecipeMatchResult result = SpecialRecipeManager.match(level, manager, refInput.process, refInput);
        helper.assertTrue(result instanceof SpecialRecipeMatchResult.Matched,
                "fallback 配方应匹配成功，实际 " + result);

        expectOutput(helper, level, manager, candidate, Items.FIREWORK_ROCKET, 3, "fallback 网格无关匹配");
        helper.succeed();
    }

    /** fallback 排布兜底（分支 2 兜底）：仍有图案、但类型不是 ShapedRecipe 的模组配方。 */
    public static void craftTableFallbackShaped(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager manager = level.recipeAccess();

        helper.assertTrue(findCraftingRecipe(manager, FALLBACK_SHAPED_RECIPE) != null,
                "应存在 " + FALLBACK_SHAPED_RECIPE);

        ItemStack chipStack = CraftTableChipItem.getStack(FALLBACK_SHAPED_RECIPE, List.of(
                new ItemStack(ItemRegistry.INACTIVATED_ETHER.get()),
                new ItemStack(ItemRegistry.GOLD_SCREW.get()),
                new ItemStack(ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_1.get())));
        // 3×3 图案：# ×4、B ×4、A ×1；各输入格子用数量补齐
        EtherFactoryMultiStepInput candidate = layout(helper, chipStack, List.of(
                new ItemStack(ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_1.get(), 1),
                new ItemStack(ItemRegistry.GOLD_SCREW.get(), 4),
                new ItemStack(ItemRegistry.INACTIVATED_ETHER.get(), 4)));
        EtherFactoryRecipeInput refInput = refInput(candidate);

        SpecialRecipeMatchResult result = SpecialRecipeManager.match(level, manager, refInput.process, refInput);
        helper.assertTrue(result instanceof SpecialRecipeMatchResult.Matched,
                "模组有图案配方应匹配成功，实际 " + result);

        expectOutput(helper, level, manager, candidate,
                ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_2.get(), 1, "fallback 排布兜底");
        helper.succeed();
    }

    /** tagUpdate 后仍可重新匹配（配方按次构建，不再按配方 id 复用实例）。 */
    public static void craftTableReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager manager = level.recipeAccess();

        CraftingRecipe target = findCraftingRecipe(manager, SHAPELESS_RECIPE);
        helper.assertTrue(target != null, "应存在无序合成配方 " + SHAPELESS_RECIPE);
        ItemStack inputStack = sampleFirstIngredient(target);
        ItemStack chipStack = CraftTableChipItem.getStack(SHAPELESS_RECIPE, List.of());

        EtherFactoryMultiStepInput candidate = layout(helper, chipStack, List.of(inputStack));
        EtherFactoryRecipeInput refInput = refInput(candidate);

        SpecialRecipeMatchResult first = SpecialRecipeManager.match(level, manager, refInput.process, refInput);
        helper.assertTrue(first instanceof SpecialRecipeMatchResult.Matched,
                "首次匹配应成功，实际 " + first);

        SpecialRecipeManager.onReload(manager);

        SpecialRecipeMatchResult second = SpecialRecipeManager.match(level, manager, refInput.process, refInput);
        helper.assertTrue(second instanceof SpecialRecipeMatchResult.Matched,
                "tagUpdate 后应仍能匹配，实际 " + second);
        expectOutput(helper, level, manager, candidate, Items.OAK_PLANKS, 4, "tagUpdate 后");
        helper.succeed();
    }

    // ------------------------------------------------------------------ 辅助

    private static void expectNoMatch(GameTestHelper helper, ServerLevel level, RecipeManager manager,
                                      ItemStack chipStack, List<ItemStack> inputs, String what) {
        EtherFactoryMultiStepInput candidate = layout(helper, chipStack, inputs);
        EtherFactoryRecipeInput refInput = refInput(candidate);

        SpecialRecipeMatchResult result = SpecialRecipeManager.match(level, manager, refInput.process, refInput);
        helper.assertTrue(result instanceof SpecialRecipeMatchResult.NoMatch,
                what + " 应为 NoMatch，实际 " + result);

        Optional<MultiStepMatchIO> matched = EtherProcessRecipeManager.getRecipe(level, manager, candidate);
        helper.assertTrue(matched.isPresent() && !matched.get().success(),
                what + " 的完整管线应严格失败");
    }

    private static void expectOutput(GameTestHelper helper, ServerLevel level, RecipeManager manager,
                                     EtherFactoryMultiStepInput candidate, Item expected, int expectedCount,
                                     String what) {
        Optional<MultiStepMatchIO> matched = EtherProcessRecipeManager.getRecipe(level, manager, candidate);
        helper.assertTrue(matched.isPresent() && matched.get().success(),
                what + " 应通过完整配方管线，实际 " + matched.map(MultiStepMatchIO::success));
        List<ItemStack> outputs = matched.get().outputs();
        helper.assertTrue(!outputs.isEmpty() && outputs.getFirst().is(expected),
                what + " 的主产物应为 " + expected + "，实际 "
                        + outputs.stream().map(s -> s.getItem().toString()).toList());
        helper.assertTrue(outputs.getFirst().getCount() == expectedCount,
                what + " 的主产物数量应为 " + expectedCount + "，实际 " + outputs.getFirst().getCount());
    }

    /** 复刻 getRecipeRecurse 里构造 EtherFactoryRecipeInput 的方式。 */
    private static EtherFactoryRecipeInput refInput(EtherFactoryMultiStepInput candidate) {
        TreeRef ref = candidate.processInputTrees().getRoot().value;
        List<Integer> ids = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();
        for (var entry : ref.inputMapping().entrySet()) {
            ids.add(entry.getKey());
            stacks.add(candidate.getGlobalItem(entry.getValue()));
        }
        return new EtherFactoryRecipeInput(stacks, ids, ref.tree());
    }

    /**
     * 第 3 行通路 + 通路格 (3,3) 上的工作台芯片；按输入数量挂竖直支路：
     * 2 个输入时加第 5 行（连接格 (3,4)），3 个输入时再加第 1 行（连接格 (1,2)）。
     */
    private static EtherFactoryMultiStepInput layout(GameTestHelper helper, ItemStack chipStack, List<ItemStack> inputs) {
        helper.assertTrue(!inputs.isEmpty() && inputs.size() <= 3,
                "该布局只支持 1~3 个输入，实际 " + inputs.size());

        EtherProcessWorkingChip[][] chips = new EtherProcessWorkingChip[ROWS][COLS];
        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS; col++)
                chips[row][col] = new EtherProcessWorkingChip(
                        ProcessChipItem.getStackFor(EtherCraft.id("separator_chip")));

        for (int col = 0; col < COLS; col++) chips[CORRIDOR_ROW][col] = null;
        chips[CORRIDOR_ROW][CHIP_COL] = new EtherProcessWorkingChip(chipStack);
        if (inputs.size() >= 2) {
            chips[CORRIDOR_ROW + 1][CHIP_COL] = null;
            for (int col = 0; col <= CHIP_COL; col++) chips[LOW_BRANCH_ROW][col] = null;
        }
        if (inputs.size() >= 3) {
            chips[CORRIDOR_ROW - 1][1] = null;
            for (int col = 0; col <= 1; col++) chips[HIGH_BRANCH_ROW][col] = null;
        }

        SimpleContainer input = new SimpleContainer(ROWS);
        input.setItem(CORRIDOR_ROW, inputs.get(0));
        if (inputs.size() >= 2) input.setItem(LOW_BRANCH_ROW, inputs.get(1));
        if (inputs.size() >= 3) input.setItem(HIGH_BRANCH_ROW, inputs.get(2));

        EtherProcessorRecipeUtil.FactoryStructure structure =
                EtherProcessorRecipeUtil.processFactoryInput(ROWS, COLS, input, chips);
        helper.assertTrue(structure.recipes.size() == 1,
                "工作台布局应产生 1 个候选树，实际 " + structure.recipes.size());
        return structure.recipes.getFirst();
    }

    private static CraftingRecipe findCraftingRecipe(RecipeManager manager, Identifier id) {
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (holder.id().identifier().equals(id) && holder.value() instanceof CraftingRecipe crafting)
                return crafting;
        }
        return null;
    }

    private static ItemStack sampleFirstIngredient(CraftingRecipe crafting) {
        List<Ingredient> ingredients = crafting.placementInfo().ingredients();
        if (ingredients.isEmpty()) throw new IllegalStateException("配方没有原料: " + crafting);
        return RecipeMirrorBuilder.sample(new SizedIngredient(ingredients.getFirst(), 1));
    }

    private static EtherProcessFactoryRecipe findRecipe(RecipeManager manager, String path) {
        Identifier id = EtherCraft.id(path);
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (holder.id().identifier().equals(id) && holder.value() instanceof EtherProcessFactoryRecipe recipe)
                return recipe;
        }
        return null;
    }
}
