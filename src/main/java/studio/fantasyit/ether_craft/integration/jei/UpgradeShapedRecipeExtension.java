package studio.fantasyit.ether_craft.integration.jei;

import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import studio.fantasyit.ether_craft.recipe.crafting.UpgradeShapedRecipe;

import java.util.List;

public class UpgradeShapedRecipeExtension implements ICraftingCategoryExtension<UpgradeShapedRecipe> {

    @Override
    public int getWidth(RecipeHolder<UpgradeShapedRecipe> recipeHolder) {
        return recipeHolder.value().getPatternData().pattern().get(0).length();
    }

    @Override
    public int getHeight(RecipeHolder<UpgradeShapedRecipe> recipeHolder) {
        return recipeHolder.value().getPatternData().pattern().size();
    }

    @Override
    public List<SlotDisplay> getIngredients(RecipeHolder<UpgradeShapedRecipe> recipeHolder) {
        var display = recipeHolder.value().display().getFirst();
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            return shaped.ingredients();
        }
        return List.of();
    }
}
