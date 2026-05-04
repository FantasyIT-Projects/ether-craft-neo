package studio.fantasyit.ether_craft.recipe;

import com.mojang.datafixers.util.Either;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public record DelayedIngredient(Either<IngredientSerializer.SizedIngredientLike, SizedIngredient> ingredient) {
    public SizedIngredient toIngredient() {
        if (ingredient.left().isPresent())
            return ingredient.left().get().toIngredient();
        return ingredient.right().get();
    }

    public static DelayedIngredient of(ItemLike ingredientItemStack) {
        return of(SizedIngredient.of(ingredientItemStack, 1));
    }

    public static DelayedIngredient of(SizedIngredient ingredient) {
        return new DelayedIngredient(Either.right(ingredient));
    }
}
