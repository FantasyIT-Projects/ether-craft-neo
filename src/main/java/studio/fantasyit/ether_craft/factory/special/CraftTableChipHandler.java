package studio.fantasyit.ether_craft.factory.special;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.item.CraftTableChipData;
import studio.fantasyit.ether_craft.item.CraftTableChipItem;
import studio.fantasyit.ether_craft.recipe.DelayedIngredient;
import studio.fantasyit.ether_craft.recipe.factory.EtherFactoryRecipeInput;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeJson;
import studio.fantasyit.ether_craft.recipe.factory.RecipeNode;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 工作台芯片的特殊配方处理器。
 *
 * <p>流程：找到树里的工作台芯片 → 用芯片记录的配方 id 解析原版合成配方（{@link CraftingRecipe}）→
 * 把【工厂实际输入物品】装配成合成网格 → {@code matches} 校验 → {@code assemble} 取主产物，
 * {@code getRemainingItems} 的各个 remain 作为附加产物一并输出。
 *
 * <p>网格装配按配方类型分派：
 * <ul>
 *     <li>{@link ShapedRecipe} / {@link ShapelessRecipe}：读取配方自己的原料列表（无序配方的
 *     {@code ingredients} 字段由 accesstransformer 公开），与实际输入做二分图匹配得到每个格位的输入；</li>
 *     <li>其它 {@link CraftingRecipe}（{@code CustomRecipe} 全家、{@code DyeRecipe} / {@code TransmuteRecipe}、
 *     模组自定义配方）：网格无关的匹配——芯片预存列表（需求）与实际输入（供给）做组件无关的多重集匹配
 *     （不要求一一对应，实际侧允许富余），匹配通过后按输入格子顺序装成单行网格；
 *     若单行网格过不了 {@code matches}（例如仍有图案的模组配方），再按 {@code placementInfo} 的格位排布兜底一次。</li>
 * </ul>
 *
 * <p>配方每次匹配时构建：产物与输入相关，不再按配方 id 缓存已构建的树，只缓存原版配方对象本身。
 */
public class CraftTableChipHandler implements SpecialRecipeHandler {

    /** 原版合成配方解析缓存（只缓存配方对象，与输入无关）。tagUpdate 时清空。 */
    private static final Map<Identifier, Optional<CraftingRecipe>> RECIPE_CACHE = new HashMap<>();

    /** 位置分配的搜索预算，防止病态输入导致卡死。 */
    private static final int MATCH_BUDGET = 8192;

    @Override
    public Identifier categoryId() {
        return CraftTableChipItem.CATEGORY;
    }

    @Override
    public List<SpecialRecipe> onReload(RecipeManager manager) {
        RECIPE_CACHE.clear();
        return List.of();
    }

    @Override
    public Optional<SpecialRecipe> match(TreeLike<List<Integer>, RecipeNode> tree,
                                         EtherFactoryRecipeInput input,
                                         Level level,
                                         RecipeManager recipeManager) {
        ItemStack chip = findChip(tree, ItemRegistry.CRAFT_TABLE_CHIP_ITEM.get());
        if (chip == null) return Optional.empty();
        CraftTableChipData data = chip.get(DataComponentRegistry.CRAFT_TABLE_CHIP_DATA);
        if (data == null) return Optional.empty();

        CraftingRecipe crafting = resolve(data.recipe(), recipeManager);
        if (crafting == null) return Optional.empty();

        // 工厂实际输入：每个输入格子一项
        List<ItemStack> actual = input.inputs;
        if (actual.isEmpty()) return Optional.empty();
        for (ItemStack stack : actual) {
            if (stack.isEmpty()) return Optional.empty();
        }

        Grid grid;
        if (crafting instanceof ShapedRecipe shaped) {
            grid = validated(gridFromShaped(shaped, actual), crafting, level);
        } else if (crafting instanceof ShapelessRecipe shapeless) {
            grid = validated(gridFromShapeless(shapeless, actual), crafting, level);
        } else {
            // fallback：芯片预存列表与实际输入做网格无关匹配（组件无关，实际侧允许富余）
            if (!markedSatisfiedBy(data.inputs(), actual)) return Optional.empty();
            grid = validated(gridFromInputCells(actual), crafting, level);
            if (grid == null) {
                for (Grid candidate : gridsFromPlacement(crafting, actual)) {
                    if (crafting.matches(candidate.input(), level)) {
                        grid = candidate;
                        break;
                    }
                }
            }
        }
        if (grid == null) return Optional.empty();

        ItemStack result = crafting.assemble(grid.input());
        if (result.isEmpty()) return Optional.empty();

        List<ItemStackTemplate> outputs = new ArrayList<>();
        outputs.add(ItemStackTemplate.fromNonEmptyStack(result));
        for (ItemStack remain : crafting.getRemainingItems(grid.input())) {
            if (!remain.isEmpty()) outputs.add(ItemStackTemplate.fromNonEmptyStack(remain));
        }

        return Optional.of(new SpecialRecipe(categoryId(), data.recipe(),
                buildFactoryRecipe(actual, grid.perInputCount(), outputs)));
    }

    // ------------------------------------------------------------------ 配方解析

    private static @Nullable CraftingRecipe resolve(Identifier id, RecipeManager recipeManager) {
        Optional<CraftingRecipe> cached = RECIPE_CACHE.computeIfAbsent(id, key -> {
            RecipeHolder<?> holder = recipeManager
                    .byKey(ResourceKey.create(Registries.RECIPE, key))
                    .orElse(null);
            if (holder != null && holder.value() instanceof CraftingRecipe crafting) {
                return Optional.of(crafting);
            }
            return Optional.empty();
        });
        return cached.orElse(null);
    }

    // ------------------------------------------------------------------ 网格装配

    /** 装配好的合成网格：{@code input} 供 matches/assemble 使用；{@code perInputCount[i]} 为第 i 个输入条目被消耗的数量（&gt;= 1）。 */
    private record Grid(CraftingInput input, int[] perInputCount) {
    }

    /** 位置分配结果：{@code slotSource} 为每个位置用的输入条目索引，{@code perInputCount} 为每个输入条目被占用的位置数。 */
    private record Assignment(int[] slotSource, int[] perInputCount) {
    }

    /** 位置对输入物品的接受判定（组件无关由调用方保证）。 */
    @FunctionalInterface
    private interface SlotRequirement {
        boolean accepts(int slot, ItemStack stack);
    }

    private static @Nullable Grid validated(@Nullable Grid grid, CraftingRecipe crafting, Level level) {
        return grid != null && crafting.matches(grid.input(), level) ? grid : null;
    }

    /** 有形状配方：格位由图案给出，与实际输入二分图匹配后逐格装配。 */
    private static @Nullable Grid gridFromShaped(ShapedRecipe recipe, List<ItemStack> actual) {
        List<Optional<Ingredient>> pattern = recipe.getIngredients();
        int width = recipe.getWidth();
        int height = recipe.getHeight();
        if (width <= 0 || height <= 0 || pattern.size() != width * height) return null;

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < pattern.size(); i++) {
            if (pattern.get(i).isPresent()) slots.add(i);
        }
        if (slots.isEmpty()) return null;

        Assignment assignment = matchPositions(actual, slots.size(),
                (slot, stack) -> pattern.get(slots.get(slot)).orElseThrow().test(stack));
        if (assignment == null) return null;

        List<ItemStack> cells = new ArrayList<>(pattern.size());
        for (int i = 0; i < pattern.size(); i++) cells.add(ItemStack.EMPTY);
        for (int slot = 0; slot < slots.size(); slot++) {
            cells.set(slots.get(slot), actual.get(assignment.slotSource()[slot]).copyWithCount(1));
        }
        return new Grid(CraftingInput.of(width, height, cells), assignment.perInputCount());
    }

    /** 无序配方：{@code ingredients} 字段（accesstransformer 已公开）就是每个格位需要的原料。 */
    private static @Nullable Grid gridFromShapeless(ShapelessRecipe recipe, List<ItemStack> actual) {
        List<Ingredient> ingredients = recipe.ingredients;
        if (ingredients.isEmpty()) return null;

        Assignment assignment = matchPositions(actual, ingredients.size(),
                (slot, stack) -> ingredients.get(slot).test(stack));
        if (assignment == null) return null;

        List<ItemStack> cells = new ArrayList<>(ingredients.size());
        for (int slot = 0; slot < ingredients.size(); slot++) {
            cells.add(actual.get(assignment.slotSource()[slot]).copyWithCount(1));
        }
        return new Grid(CraftingInput.of(ingredients.size(), 1, cells), assignment.perInputCount());
    }

    /** fallback 主路径：网格无关匹配通过后，按输入格子顺序装成单行网格。 */
    private static Grid gridFromInputCells(List<ItemStack> actual) {
        List<ItemStack> cells = new ArrayList<>(actual.size());
        for (ItemStack stack : actual) cells.add(stack.copyWithCount(1));
        int[] counts = new int[actual.size()];
        Arrays.fill(counts, 1);
        return new Grid(CraftingInput.of(actual.size(), 1, cells), counts);
    }

    /**
     * fallback 兜底：按 {@code placementInfo} 的格位分布装配候选网格（宽高未知时枚举因数当宽度，
     * 由调用方逐个用 {@code matches} 筛选）。用于仍有图案、但类型不是 {@link ShapedRecipe} 的配方。
     */
    private static List<Grid> gridsFromPlacement(CraftingRecipe crafting, List<ItemStack> actual) {
        PlacementInfo info = crafting.placementInfo();
        var slotToIngredient = info.slotsToIngredientIndex();
        int totalSlots = slotToIngredient.size();
        List<Ingredient> ingredients = info.ingredients();
        if (totalSlots <= 0 || ingredients.isEmpty()) return List.of();

        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < totalSlots; slot++) {
            if (slotToIngredient.getInt(slot) >= 0) slots.add(slot);
        }
        if (slots.isEmpty()) return List.of();

        Assignment assignment = matchPositions(actual, slots.size(),
                (slot, stack) -> ingredients.get(slotToIngredient.getInt(slots.get(slot))).test(stack));
        if (assignment == null) return List.of();

        List<Grid> grids = new ArrayList<>();
        for (int width = 1; width <= totalSlots; width++) {
            if (totalSlots % width != 0) continue;
            List<ItemStack> cells = new ArrayList<>(totalSlots);
            for (int i = 0; i < totalSlots; i++) cells.add(ItemStack.EMPTY);
            for (int slot = 0; slot < slots.size(); slot++) {
                cells.set(slots.get(slot), actual.get(assignment.slotSource()[slot]).copyWithCount(1));
            }
            grids.add(new Grid(CraftingInput.of(width, totalSlots / width, cells), assignment.perInputCount()));
        }
        return grids;
    }

    // ------------------------------------------------------------------ 匹配

    /**
     * 芯片预存列表（需求，count 视为重复次数）能否被实际输入（供给，count 视为可用数量）满足。
     *
     * <p>网格无关：只比较物品类型（组件无关），不要求两侧长度相等或一一对应，实际侧允许富余。
     * 二分图里只有同物品之间才连边，因此等价于按物品类型的数量比较。
     */
    private static boolean markedSatisfiedBy(List<ItemStackTemplate> marked, List<ItemStack> actual) {
        Map<Item, Integer> available = new HashMap<>();
        for (ItemStack stack : actual) {
            available.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        for (ItemStackTemplate need : marked) {
            int count = Math.max(1, need.count());
            Item item = need.item().value();
            int have = available.getOrDefault(item, 0);
            if (have < count) return false;
            available.put(item, have - count);
        }
        return true;
    }

    /**
     * 网格位置的二分图匹配（带容量与覆盖约束）：每个位置分到一个满足其原料的实际输入物品，
     * 且【每个输入条目至少要贡献一次】——下游要求配方 input 与工厂输入条目等长且数量为正。
     */
    private static @Nullable Assignment matchPositions(List<ItemStack> actual, int positions, SlotRequirement requirement) {
        int entries = actual.size();
        if (positions < entries) return null;
        int[] perInputCount = new int[entries];
        int[] slotSource = new int[positions];
        Arrays.fill(slotSource, -1);
        int[] budget = {MATCH_BUDGET};
        if (!assignSlot(0, actual, positions, requirement, perInputCount, slotSource, budget)) return null;
        return new Assignment(slotSource, perInputCount);
    }

    private static boolean assignSlot(int slot,
                                      List<ItemStack> actual,
                                      int positions,
                                      SlotRequirement requirement,
                                      int[] perInputCount,
                                      int[] slotSource,
                                      int[] budget) {
        if (--budget[0] < 0) return false;
        if (slot == positions) {
            for (int count : perInputCount) {
                if (count <= 0) return false;
            }
            return true;
        }
        int unused = 0;
        for (int count : perInputCount) {
            if (count == 0) unused++;
        }
        if (positions - slot < unused) return false;

        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < actual.size(); i++) {
            if (perInputCount[i] >= actual.get(i).getCount()) continue;
            if (!requirement.accepts(slot, actual.get(i))) continue;
            candidates.add(i);
        }
        // 尚未被使用的条目优先，其次条目序号，保证结果稳定
        candidates.sort((a, b) -> {
            int cmp = Integer.compare(perInputCount[a], perInputCount[b]);
            return cmp != 0 ? cmp : Integer.compare(a, b);
        });
        for (int candidate : candidates) {
            perInputCount[candidate]++;
            slotSource[slot] = candidate;
            if (assignSlot(slot + 1, actual, positions, requirement, perInputCount, slotSource, budget)) return true;
            perInputCount[candidate]--;
            slotSource[slot] = -1;
        }
        return false;
    }

    // ------------------------------------------------------------------ 配方构建

    private static EtherProcessFactoryRecipe buildFactoryRecipe(List<ItemStack> actual,
                                                               int[] perInputCount,
                                                               List<ItemStackTemplate> outputs) {
        List<EtherProcessRecipeJson.InputEntry> inputEntries = new ArrayList<>(actual.size());
        for (int i = 0; i < actual.size(); i++) {
            // 组件无关：只按物品类型构造原料；数量为该输入在合成网格中被占用的次数
            inputEntries.add(new EtherProcessRecipeJson.InputEntry(
                    "I" + i,
                    new SizedIngredient(Ingredient.of(actual.get(i).getItem()), perInputCount[i]),
                    "P"));
        }
        // 工序芯片用普通物品原料（忽略数据组件），否则带组件的工作台芯片永远匹配不上
        EtherProcessRecipeJson.ProcessEntry process = new EtherProcessRecipeJson.ProcessEntry(
                "P",
                List.of(DelayedIngredient.of(ItemRegistry.CRAFT_TABLE_CHIP_ITEM.get())),
                null);
        EtherProcessRecipeJson.OutputEntry output = new EtherProcessRecipeJson.OutputEntry("O", outputs);
        return new EtherProcessFactoryRecipe(new EtherProcessRecipeJson(inputEntries, output, List.of(process)));
    }

    private static @Nullable ItemStack findChip(TreeLike<List<Integer>, RecipeNode> tree, Item item) {
        for (TreeLike.TreeNode<List<Integer>, RecipeNode> node : tree.getNodes()) {
            for (TreeLike.TreeEdge<List<Integer>, RecipeNode> edge : node.edges) {
                for (ItemStack stack : edge.value.input()) {
                    if (stack.is(item)) return stack;
                }
            }
        }
        return null;
    }
}
