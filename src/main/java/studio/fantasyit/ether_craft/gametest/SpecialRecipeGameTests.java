package studio.fantasyit.ether_craft.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 工作台芯片 / 特殊配方预匹配测试。
 *
 * <p>布局（7×7，通路在第 3 行）：工作台芯片放在<b>通路格本身</b>上
 * （默认元：separate=false / effectSelf=true / effectSide=false），
 * 输入从第 3 行左侧流入，输出在第 3 行右侧。
 */
public final class SpecialRecipeGameTests {
    private SpecialRecipeGameTests() {
    }

    private static final int ROWS = 7;
    private static final int COLS = 7;
    private static final int CORRIDOR_ROW = 3;
    private static final int CHIP_COL = 3;
    private static final Identifier SINGLE_INPUT_RECIPE = Identifier.withDefaultNamespace("oak_planks");

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

    /** 工作台芯片放在通路上 → Matched，且整条工厂管线成功并产出目标物品。 */
    public static void craftTableMatched(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager manager = level.recipeAccess();

        CraftingRecipe target = findSingleInputRecipe(manager);
        helper.assertTrue(target != null, "应存在单原料合成配方 " + SINGLE_INPUT_RECIPE);

        ItemStack inputStack = sampleFirstIngredient(target);
        ItemStack chipStack = CraftTableChipItem.getStack(SINGLE_INPUT_RECIPE, List.of());

        helper.assertTrue(EtherProcessChipManager.get(chipStack) != null,
                "工作台芯片应有对应的数据包芯片记录（ether_process_chip/craft_table_chip.json）");
        helper.assertTrue(EtherProcessChipManager.get(chipStack).effect().effectSelf()
                        && !EtherProcessChipManager.get(chipStack).effect().separate()
                        && !EtherProcessChipManager.get(chipStack).effect().effectSide(),
                "工作台芯片默认元应为：影响当前格子、不阻挡通路、不影响周围格子");

        EtherFactoryMultiStepInput candidate = layout(helper, chipStack, inputStack);
        EtherFactoryRecipeInput refInput = refInput(candidate);

        SpecialRecipeMatchResult direct = SpecialRecipeManager.match(level, manager, refInput.process, refInput);
        helper.assertTrue(direct instanceof SpecialRecipeMatchResult.Matched,
                "含工作台芯片的树应匹配到特殊配方，实际 " + direct);

        Optional<MultiStepMatchIO> matched = EtherProcessRecipeManager.getRecipe(level, manager, candidate);
        helper.assertTrue(matched.isPresent() && matched.get().success(),
                "工作台布局应通过完整配方管线");
        List<ItemStack> outputs = matched.get().outputs();
        helper.assertTrue(!outputs.isEmpty() && outputs.getFirst().is(Items.OAK_PLANKS),
                "产物应为橡木木板，实际 " + outputs.stream().map(s -> s.getItem().toString()).toList());
        helper.succeed();
    }

    /** 芯片未配置 / 配方 id 无效 → NoMatch（严格失败，不回退常规匹配）。 */
    public static void craftTableNoMatch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager manager = level.recipeAccess();

        CraftingRecipe target = findSingleInputRecipe(manager);
        helper.assertTrue(target != null, "应存在单原料合成配方 " + SINGLE_INPUT_RECIPE);
        ItemStack inputStack = sampleFirstIngredient(target);

        // 1) 完全没有数据组件的工作台芯片
        ItemStack unset = ProcessChipItem.getStackFor(CraftTableChipItem.CHIP_ID);
        expectNoMatch(helper, level, manager, unset, inputStack, "未配置的工作台芯片");

        // 2) 数据组件指向不存在的配方
        ItemStack bogus = CraftTableChipItem.getStack(Identifier.fromNamespaceAndPath("ether_craft", "not_a_recipe"), List.of());
        expectNoMatch(helper, level, manager, bogus, inputStack, "配方 id 无效的工作台芯片");

        helper.succeed();
    }

    /** inputs 只用于展示/编辑，不参与匹配。 */
    public static void craftTableInputsIgnored(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager manager = level.recipeAccess();

        CraftingRecipe target = findSingleInputRecipe(manager);
        helper.assertTrue(target != null, "应存在单原料合成配方 " + SINGLE_INPUT_RECIPE);
        ItemStack inputStack = sampleFirstIngredient(target);

        // 故意把 inputs 填成与配方无关的物品
        ItemStack chipStack = CraftTableChipItem.getStack(SINGLE_INPUT_RECIPE,
                List.of(new ItemStackTemplate(Items.DIAMOND), new ItemStackTemplate(Items.BEDROCK)));
        EtherFactoryMultiStepInput candidate = layout(helper, chipStack, inputStack);
        EtherFactoryRecipeInput refInput = refInput(candidate);

        SpecialRecipeMatchResult result = SpecialRecipeManager.match(level, manager, refInput.process, refInput);
        helper.assertTrue(result instanceof SpecialRecipeMatchResult.Matched,
                "inputs 与配方无关时仍应匹配成功，实际 " + result);
        helper.succeed();
    }

    /** 树按配方 id 惰性构建并缓存；tagUpdate 后缓存失效。 */
    public static void craftTableCacheReuse(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager manager = level.recipeAccess();

        CraftingRecipe target = findSingleInputRecipe(manager);
        helper.assertTrue(target != null, "应存在单原料合成配方 " + SINGLE_INPUT_RECIPE);
        ItemStack inputStack = sampleFirstIngredient(target);
        ItemStack chipStack = CraftTableChipItem.getStack(SINGLE_INPUT_RECIPE, List.of());

        EtherFactoryMultiStepInput candidate = layout(helper, chipStack, inputStack);
        EtherFactoryRecipeInput refInput = refInput(candidate);

        EtherProcessFactoryRecipe first = matchedRecipe(helper, level, manager, refInput);
        EtherProcessFactoryRecipe second = matchedRecipe(helper, level, manager, refInput);
        helper.assertTrue(first == second,
                "同一配方 id 应复用同一棵树实例（惰性缓存）");

        SpecialRecipeManager.onReload(manager);
        EtherProcessFactoryRecipe third = matchedRecipe(helper, level, manager, refInput);
        helper.assertTrue(third != first,
                "tagUpdate 后缓存应失效并重新构建");
        helper.succeed();
    }

    // ------------------------------------------------------------------ 辅助

    private static void expectNoMatch(GameTestHelper helper, ServerLevel level, RecipeManager manager,
                                      ItemStack chipStack, ItemStack inputStack, String what) {
        EtherFactoryMultiStepInput candidate = layout(helper, chipStack, inputStack);
        EtherFactoryRecipeInput refInput = refInput(candidate);

        SpecialRecipeMatchResult result = SpecialRecipeManager.match(level, manager, refInput.process, refInput);
        helper.assertTrue(result instanceof SpecialRecipeMatchResult.NoMatch,
                what + " 应为 NoMatch，实际 " + result);

        Optional<MultiStepMatchIO> matched = EtherProcessRecipeManager.getRecipe(level, manager, candidate);
        helper.assertTrue(matched.isPresent() && !matched.get().success(),
                what + " 的完整管线应严格失败");
    }

    private static EtherProcessFactoryRecipe matchedRecipe(GameTestHelper helper, ServerLevel level,
                                                           RecipeManager manager, EtherFactoryRecipeInput refInput) {
        SpecialRecipeMatchResult result = SpecialRecipeManager.match(level, manager, refInput.process, refInput);
        helper.assertTrue(result instanceof SpecialRecipeMatchResult.Matched,
                "应匹配到特殊配方，实际 " + result);
        return ((SpecialRecipeMatchResult.Matched) result).recipe().recipe();
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

    /** 第 3 行通路 + 通路格 (3,3) 上的工作台芯片。 */
    private static EtherFactoryMultiStepInput layout(GameTestHelper helper, ItemStack chipStack, ItemStack inputStack) {
        EtherProcessWorkingChip[][] chips = new EtherProcessWorkingChip[ROWS][COLS];
        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS; col++)
                chips[row][col] = new EtherProcessWorkingChip(ProcessChipItem.getStackFor(studio.fantasyit.ether_craft.EtherCraft.id("separator_chip")));

        for (int col = 0; col < COLS; col++) chips[CORRIDOR_ROW][col] = null;
        chips[CORRIDOR_ROW][CHIP_COL] = new EtherProcessWorkingChip(chipStack);

        SimpleContainer input = new SimpleContainer(ROWS);
        input.setItem(CORRIDOR_ROW, inputStack);

        EtherProcessorRecipeUtil.FactoryStructure structure =
                EtherProcessorRecipeUtil.processFactoryInput(ROWS, COLS, input, chips);
        helper.assertTrue(structure.recipes.size() == 1,
                "工作台布局应产生 1 个候选树，实际 " + structure.recipes.size());
        return structure.recipes.getFirst();
    }

    private static CraftingRecipe findSingleInputRecipe(RecipeManager manager) {
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (!holder.id().identifier().equals(SINGLE_INPUT_RECIPE)) continue;
            if (!(holder.value() instanceof CraftingRecipe crafting)) return null;
            return crafting;
        }
        return null;
    }

    private static ItemStack sampleFirstIngredient(CraftingRecipe crafting) {
        List<Ingredient> ingredients = crafting.placementInfo().ingredients();
        if (ingredients.isEmpty()) throw new IllegalStateException("配方没有原料: " + SINGLE_INPUT_RECIPE);
        return RecipeMirrorBuilder.sample(new SizedIngredient(ingredients.getFirst(), 1));
    }

    private static EtherProcessFactoryRecipe findRecipe(RecipeManager manager, String path) {
        Identifier id = studio.fantasyit.ether_craft.EtherCraft.id(path);
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (holder.id().identifier().equals(id) && holder.value() instanceof EtherProcessFactoryRecipe recipe)
                return recipe;
        }
        return null;
    }
}
