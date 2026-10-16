package studio.fantasyit.ether_craft.factory.special;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.recipe.factory.EtherFactoryRecipeInput;
import studio.fantasyit.ether_craft.recipe.factory.RecipeNode;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 特殊配方全局管理器：在"concat 芯片切分后的单树配方匹配"之前做一次提前匹配。
 *
 * <p>判定规则：
 * <ul>
 *     <li>树里没有出现任何已注册的特殊芯片物品 → {@link SpecialRecipeMatchResult.NotSpecial}；</li>
 *     <li>出现了特殊芯片物品，但对应处理器返回空 → {@link SpecialRecipeMatchResult.NoMatch}；</li>
 *     <li>处理器返回配方 → {@link SpecialRecipeMatchResult.Matched}。</li>
 * </ul>
 * 一棵树里出现多颗特殊芯片时，只取遍历到的第一颗。
 */
public final class SpecialRecipeManager {
    private SpecialRecipeManager() {
    }

    /** 特殊芯片物品 → 处理器。 */
    private static final Map<Item, SpecialRecipeHandler> HANDLERS = new LinkedHashMap<>();
    /** 分类 id → (配方 id → 配方)。 */
    private static final Map<Identifier, Map<Identifier, SpecialRecipe>> RECIPES_BY_CATEGORY = new LinkedHashMap<>();

    public static void registerHandler(Item chipItem, SpecialRecipeHandler handler) {
        HANDLERS.put(chipItem, handler);
    }

    public static void collectHandlers() {
        HANDLERS.clear();
        RECIPES_BY_CATEGORY.clear();
        registerHandler(ItemRegistry.CRAFT_TABLE_CHIP_ITEM.get(), new CraftTableChipHandler());
    }

    public static void onReload(RecipeManager manager) {
        RECIPES_BY_CATEGORY.clear();
        for (SpecialRecipeHandler handler : HANDLERS.values()) {
            for (SpecialRecipe recipe : handler.onReload(manager)) {
                RECIPES_BY_CATEGORY
                        .computeIfAbsent(recipe.categoryId(), k -> new LinkedHashMap<>())
                        .put(recipe.id(), recipe);
            }
        }
    }

    public static @Nullable SpecialRecipe getRecipe(Identifier categoryId, Identifier id) {
        Map<Identifier, SpecialRecipe> recipes = RECIPES_BY_CATEGORY.get(categoryId);
        return recipes == null ? null : recipes.get(id);
    }

    public static SpecialRecipeMatchResult match(Level level,
                                                 RecipeManager recipeManager,
                                                 TreeLike<List<Integer>, RecipeNode> tree,
                                                 EtherFactoryRecipeInput input) {
        for (TreeLike.TreeNode<List<Integer>, RecipeNode> node : tree.getNodes()) {
            for (TreeLike.TreeEdge<List<Integer>, RecipeNode> edge : node.edges) {
                for (ItemStack chip : edge.value.input()) {
                    SpecialRecipeHandler handler = HANDLERS.get(chip.getItem());
                    if (handler == null) continue;
                    return handler.match(tree, input, level, recipeManager)
                            .<SpecialRecipeMatchResult>map(SpecialRecipeMatchResult.Matched::new)
                            .orElseGet(SpecialRecipeMatchResult.NoMatch::new);
                }
            }
        }
        return new SpecialRecipeMatchResult.NotSpecial();
    }
}
