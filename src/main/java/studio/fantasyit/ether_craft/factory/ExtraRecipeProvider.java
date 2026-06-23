package studio.fantasyit.ether_craft.factory;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeManager;

import java.util.List;

public interface ExtraRecipeProvider {
    Identifier getCategoryId();

    ItemLike getIcon();

    List<EtherProcessRecipeManager.ExtraRecipe> generate(RecipeManager manager);
}
