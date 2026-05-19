package studio.fantasyit.ether_craft.factory.special;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.factory.EtherProcessRecipeManager;
import studio.fantasyit.ether_craft.recipe.DelayedIngredient;
import studio.fantasyit.ether_craft.recipe.IngredientSerializer;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeJson;

import java.util.List;
import java.util.function.Function;

public class ExtraFurnaceRecipe implements Function<RecipeManager, List<EtherProcessRecipeManager.ExtraRecipe>> {
    public static final Identifier CATEGORY = EtherCraft.id("special/furnace");

    @Override
    public List<EtherProcessRecipeManager.ExtraRecipe> apply(RecipeManager recipeManager) {
        return recipeManager.getRecipes()
                .stream()
                .filter(r -> r.value().getType() == RecipeType.BLASTING)
                .map(t -> new EtherProcessRecipeManager.ExtraRecipe(
                        CATEGORY,
                        t.id().identifier(),
                        new EtherProcessFactoryRecipe(getFor((BlastingRecipe) t.value()))
                ))
                .toList();
    }

    public EtherProcessRecipeJson getFor(BlastingRecipe r) {
        EtherProcessRecipeJson.InputEntry input = new EtherProcessRecipeJson.InputEntry(
                "I",
                new SizedIngredient(r.input(), 1),
                "P"
        );
        EtherProcessRecipeJson.OutputEntry output = new EtherProcessRecipeJson.OutputEntry(
                "O",
                List.of(r.result())
        );
        EtherProcessRecipeJson.ProcessEntry process = new EtherProcessRecipeJson.ProcessEntry(
                "P",
                List.of(
                        DelayedIngredient.of(new IngredientSerializer.ChipRecord(EtherCraft.id("heating_chip"))),
                        DelayedIngredient.of(new IngredientSerializer.ChipRecord(EtherCraft.id("heating_chip")))
                ),
                "O"
        );
        return new EtherProcessRecipeJson(
                List.of(input),
                output,
                List.of(process)
        );
    }
}
