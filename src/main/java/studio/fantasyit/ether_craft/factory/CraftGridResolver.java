package studio.fantasyit.ether_craft.factory;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.item.CraftTableChipData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 工作台网格（3×3 标记格）与原版合成配方之间的桥。
 *
 * <ul>
 *     <li>{@link #markFromRecipe}：配方 → 9 格图案（JEI「+」转移时用，客户端即可）；</li>
 *     <li>{@link #resolve}：9 格图案 → 命中的配方（服务端，写芯片时用）；</li>
 *     <li>{@link #fill}：芯片里存的 9 格图案 → 标记格（服务端回填时用）。</li>
 * </ul>
 */
public final class CraftGridResolver {
    public static final int SIZE = 3;
    public static final int CELLS = SIZE * SIZE;

    private CraftGridResolver() {
    }

    /** 9 格图案 → 命中的配方；空网格或无命中返回 {@code null}。 */
    public static @Nullable CraftTableChipData resolve(Level level, List<ItemStack> grid) {
        if (!(level.recipeAccess() instanceof RecipeManager manager)) return null;

        List<ItemStack> cells = normalize(grid);
        if (cells.stream().allMatch(ItemStack::isEmpty)) return null;

        CraftingInput input = CraftingInput.of(SIZE, SIZE, cells);
        for (RecipeHolder<?> holder : manager.getRecipes())
            if (holder.value() instanceof CraftingRecipe recipe && recipe.matches(input, level))
                return new CraftTableChipData(holder.id().identifier(), cells);
        return null;
    }

    /** 把芯片里存的 9 格图案逐格还原（空气也照常清空）。 */
    public static void fill(CraftTableChipData data, ItemFilter grid) {
        List<ItemStack> cells = normalize(data.grid());
        for (int i = 0; i < CELLS; i++) grid.setItemNoTrigger(i, cells.get(i));
    }

    /** 把一条原版合成配方按图案写成 9 格标记。 */
    public static void markFromRecipe(Level level, CraftingRecipe recipe, ItemFilter grid) {
        List<ItemStack> cells = cellsOf(level, recipe);
        for (int i = 0; i < CELLS; i++)
            grid.setItemNoTrigger(i, cells == null ? ItemStack.EMPTY : cells.get(i));
    }

    // ------------------------------------------------------------------ 内部

    /** 补齐/裁剪成 9 格并把数量压到 1。 */
    private static List<ItemStack> normalize(List<ItemStack> grid) {
        List<ItemStack> cells = new ArrayList<>(CELLS);
        for (int i = 0; i < CELLS; i++) {
            ItemStack stack = i < grid.size() ? grid.get(i) : ItemStack.EMPTY;
            cells.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        }
        return cells;
    }

    private static @Nullable List<ItemStack> cellsOf(Level level, CraftingRecipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            List<ItemStack> cells = cellsOfShaped(shaped);
            if (cells != null && matches(level, cells, recipe)) return cells;
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            List<ItemStack> cells = cellsOfShapeless(shapeless);
            if (cells != null && matches(level, cells, recipe)) return cells;
        }
        return cellsOfPlacement(level, recipe);
    }

    private static @Nullable List<ItemStack> cellsOfShaped(ShapedRecipe recipe) {
        int width = recipe.getWidth();
        int height = recipe.getHeight();
        List<Optional<Ingredient>> pattern = recipe.getIngredients();
        if (width <= 0 || height <= 0 || width > SIZE || height > SIZE) return null;
        if (pattern.size() < width * height) return null;

        List<ItemStack> cells = normalize(List.of());
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Optional<ItemStack> sample = pattern.get(y * width + x).flatMap(CraftGridResolver::sample);
                if (sample.isPresent()) cells.set(y * SIZE + x, sample.get());
            }
        }
        return cells;
    }

    private static List<ItemStack> cellsOfShapeless(ShapelessRecipe recipe) {
        List<ItemStack> cells = normalize(List.of());
        int index = 0;
        for (Ingredient ingredient : recipe.ingredients) {
            if (index >= CELLS) break;
            Optional<ItemStack> sample = sample(ingredient);
            if (sample.isPresent()) cells.set(index++, sample.get());
        }
        return cells;
    }

    /**
     * 兜底：按 {@link PlacementInfo} 枚举宽度与平移，取第一种能通过 {@code matches} 的布局。
     * 用于形状信息不由 {@link ShapedRecipe}/{@link ShapelessRecipe} 描述的配方。
     */
    private static @Nullable List<ItemStack> cellsOfPlacement(Level level, CraftingRecipe recipe) {
        PlacementInfo info = recipe.placementInfo();
        var slotToIngredient = info.slotsToIngredientIndex();
        List<Ingredient> ingredients = info.ingredients();
        int total = slotToIngredient.size();
        if (total <= 0 || total > CELLS || ingredients.isEmpty()) return null;

        List<ItemStack> perSlot = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            int index = slotToIngredient.getInt(i);
            perSlot.add(index >= 0 && index < ingredients.size()
                    ? sample(ingredients.get(index)).orElse(ItemStack.EMPTY)
                    : ItemStack.EMPTY);
        }

        for (int width = 1; width <= total; width++) {
            if (total % width != 0) continue;
            int height = total / width;
            if (width > SIZE || height > SIZE) continue;
            for (int ox = 0; ox + width <= SIZE; ox++) {
                for (int oy = 0; oy + height <= SIZE; oy++) {
                    List<ItemStack> cells = normalize(List.of());
                    for (int i = 0; i < total; i++)
                        if (!perSlot.get(i).isEmpty())
                            cells.set((oy + i / width) * SIZE + (ox + i % width), perSlot.get(i));
                    if (matches(level, cells, recipe)) return cells;
                }
            }
        }
        return null;
    }

    private static boolean matches(Level level, List<ItemStack> cells, CraftingRecipe recipe) {
        return recipe.matches(CraftingInput.of(SIZE, SIZE, cells), level);
    }

    private static Optional<ItemStack> sample(Ingredient ingredient) {
        return ingredient.items().findFirst().map(holder -> new ItemStack(holder.value()));
    }
}
