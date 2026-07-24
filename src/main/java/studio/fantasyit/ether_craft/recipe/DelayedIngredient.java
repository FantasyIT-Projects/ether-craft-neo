package studio.fantasyit.ether_craft.recipe;

import com.mojang.datafixers.util.Either;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.Objects;

public final class DelayedIngredient {
    private final Either<IngredientSerializer.SizedIngredientLike, SizedIngredient> ingredient;
    private volatile SizedIngredient cached;

    public DelayedIngredient(Either<IngredientSerializer.SizedIngredientLike, SizedIngredient> ingredient) {
        this.ingredient = ingredient;
    }

    public Either<IngredientSerializer.SizedIngredientLike, SizedIngredient> ingredient() {
        return ingredient;
    }

    public SizedIngredient toIngredient() {
        SizedIngredient c = cached;
        if (c != null) return c;
        synchronized (this) {
            if (cached == null) {
                if (ingredient.left().isPresent())
                    cached = ingredient.left().get().toIngredient();
                else
                    cached = ingredient.right().get();
            }
            return cached;
        }
    }

    public static DelayedIngredient of(ItemLike ingredientItemStack) {
        return of(SizedIngredient.of(ingredientItemStack, 1));
    }

    public static DelayedIngredient of(SizedIngredient ingredient) {
        return new DelayedIngredient(Either.right(ingredient));
    }

    public static DelayedIngredient of(IngredientSerializer.SizedIngredientLike ingredient) {
        return new DelayedIngredient(Either.left(ingredient));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DelayedIngredient that)) return false;
        return Objects.equals(ingredient, that.ingredient);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ingredient);
    }

    @Override
    public String toString() {
        return "DelayedIngredient[" + ingredient + "]";
    }
}
