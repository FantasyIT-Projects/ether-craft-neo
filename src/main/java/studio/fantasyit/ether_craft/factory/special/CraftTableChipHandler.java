package studio.fantasyit.ether_craft.factory.special;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 工作台芯片的特殊配方处理器。
 *
 * <p>把原版合成配方（{@code minecraft:crafting}）转成工厂树：
 * 一般输入（配方原料）→ 工作台（工序节点，芯片 = 工作台芯片）→ 一般输出（配方产物）。
 *
 * <p>树按需惰性构建，并以原版配方 id 为键缓存；tagUpdate 时清空。
 */
public class CraftTableChipHandler implements SpecialRecipeHandler {

    private static final Map<Identifier, Optional<EtherProcessFactoryRecipe>> CACHE = new HashMap<>();

    @Override
    public Identifier categoryId() {
        return CraftTableChipItem.CATEGORY;
    }

    @Override
    public List<SpecialRecipe> onReload(RecipeManager manager) {
        CACHE.clear();
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

        EtherProcessFactoryRecipe recipe = CACHE
                .computeIfAbsent(data.recipe(), id -> build(id, level, recipeManager))
                .orElse(null);
        if (recipe == null) return Optional.empty();
        if (!recipe.matches(input, level)) return Optional.empty();
        return Optional.of(new SpecialRecipe(categoryId(), data.recipe(), recipe));
    }

    /** 原版合成配方 → 工厂树。 */
    private static Optional<EtherProcessFactoryRecipe> build(Identifier id, Level level, RecipeManager recipeManager) {
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
        return recipeManager.byKey(key)
                .filter(holder -> holder.value() instanceof CraftingRecipe)
                .flatMap(holder -> buildTree((CraftingRecipe) holder.value(), level));
    }

    private static Optional<EtherProcessFactoryRecipe> buildTree(CraftingRecipe crafting, Level level) {
        List<Ingredient> ingredients = crafting.placementInfo().ingredients();
        ItemStackTemplate result = resultOf(crafting, level);
        if (result == null || ingredients.isEmpty()) return Optional.empty();

        List<EtherProcessRecipeJson.InputEntry> inputs = new ArrayList<>(ingredients.size());
        for (int i = 0; i < ingredients.size(); i++) {
            inputs.add(new EtherProcessRecipeJson.InputEntry("I" + i, new SizedIngredient(ingredients.get(i), 1), "P"));
        }
        // 工序芯片用普通物品原料（忽略数据组件），否则带组件的工作台芯片永远匹配不上
        EtherProcessRecipeJson.ProcessEntry process = new EtherProcessRecipeJson.ProcessEntry(
                "P",
                List.of(DelayedIngredient.of(ItemRegistry.CRAFT_TABLE_CHIP_ITEM.get())),
                null);
        EtherProcessRecipeJson.OutputEntry output = new EtherProcessRecipeJson.OutputEntry("O", List.of(result));
        return Optional.of(new EtherProcessFactoryRecipe(
                new EtherProcessRecipeJson(inputs, output, List.of(process))));
    }

    private static @Nullable ItemStackTemplate resultOf(CraftingRecipe crafting, Level level) {
        //FIXME 傻逼大肥鱼。此处需要重写
        for (RecipeDisplay display : crafting.display()) {
            SlotDisplay result = null;
            if (display instanceof ShapedCraftingRecipeDisplay shaped) {
                result = shaped.result();
            } else if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
                result = shapeless.result();
            }
            if (result == null) continue;
            ItemStack stack = result.resolveForFirstStack(SlotDisplayContext.fromLevel(level));
            if (!stack.isEmpty()) return ItemStackTemplate.fromNonEmptyStack(stack);
        }
        return null;
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
