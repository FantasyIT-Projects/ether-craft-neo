package studio.fantasyit.ether_craft.recipe.plating;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.plating.data.PlatingEffectFormula;
import studio.fantasyit.ether_craft.plating.data.ProgressingPlatingData;
import studio.fantasyit.ether_craft.register.RecipeSerializerRegistry;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PlatingRecipe implements Recipe<PlatingRecipeInput> {
    public final List<SizedIngredient> input;
    public final Identifier effectId;
    public final PlatingEffectFormula values;
    public final Ingredient filter;

    public static final MapCodec<PlatingRecipe> CODEC = PlatingRecipeJson.CODEC.xmap(
            PlatingRecipe::new,
            r -> new PlatingRecipeJson(r.input, r.effectId, r.filter, r.values)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PlatingRecipe> STREAM_CODEC =
            PlatingRecipeJson.STREAM_CODEC.map(PlatingRecipe::new, r -> new PlatingRecipeJson(r.input, r.effectId, r.filter, r.values));

    public PlatingRecipe(PlatingRecipeJson json) {
        this.input = json.input();
        this.effectId = json.effect();
        this.filter = json.filter();
        this.values = json.values();
    }

    public boolean matchesFilter(ItemStack stack) {
        return filter.test(stack);
    }

    public boolean matches(List<ItemStack> availableItems) {
        Map<ItemStack, Long> availableMap = availableItems.stream()
                .filter(s -> !s.isEmpty())
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.summingLong(ItemStack::getCount)
                ));
        return canSatisfy(new HashMap<>(availableMap));
    }

    private boolean canSatisfy(Map<ItemStack, Long> available) {
        for (SizedIngredient ingredient : input) {
            int needed = ingredient.count();
            for (var entry : available.entrySet()) {
                if (entry.getValue() > 0 && ingredient.ingredient().test(entry.getKey())) {
                    long take = Math.min(needed, entry.getValue());
                    needed -= (int) take;
                    entry.setValue(entry.getValue() - take);
                    if (needed == 0) break;
                }
            }
            if (needed > 0) return false;
        }
        return true;
    }

    public int inputSize() {
        return input.size();
    }

    @Override
    public boolean matches(PlatingRecipeInput input, Level level) {
        return matches(input.items);
    }

    @Override
    public ItemStack assemble(PlatingRecipeInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public @NotNull String group() {
        return "";
    }

    @Override
    public @NotNull RecipeSerializer<@NotNull PlatingRecipe> getSerializer() {
        return RecipeSerializerRegistry.PLATING_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<PlatingRecipeInput>> getType() {
        return RecipeTypeRegistry.PLATING_RECIPE.get();
    }

    @Override
    public @NotNull PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public @NotNull RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    public ProgressingPlatingData makeProcessing() {
        return new ProgressingPlatingData(effectId, values);
    }
}