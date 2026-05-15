package studio.fantasyit.ether_craft.recipe.node;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.register.RecipeSerializerRegistry;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;

import java.util.ArrayList;
import java.util.List;

public class NodeProcessRecipe implements Recipe<@NotNull NodeProcessRecipeInput> {
    public final List<SizedIngredient> ingredients;
    public final ItemStackTemplate result;
    public final int etherCost;
    public final NodeProcessRecipeJson json;

    public static final MapCodec<NodeProcessRecipe> CODEC = NodeProcessRecipeJson.MAP_CODEC.xmap(
            NodeProcessRecipe::new,
            t -> t.json
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, NodeProcessRecipe> STREAM_CODEC = StreamCodec.composite(
            NodeProcessRecipeJson.STREAM_CODEC,
            t -> t.json,
            NodeProcessRecipe::new
    );

    public NodeProcessRecipe(NodeProcessRecipeJson json) {
        this.json = json;
        this.ingredients = json.ingredients();
        this.result = json.result();
        this.etherCost = json.etherCost();
    }

    public boolean matchesResult(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(stack, result.create());
    }

    public boolean matchesSubset(List<ItemStack> inputs) {
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack s : inputs) {
            if (!s.isEmpty())
                remaining.add(s.copy());
        }
        for (SizedIngredient ingredient : ingredients) {
            int needed = ingredient.count();
            for (ItemStack item : remaining) {
                if (ingredient.ingredient().test(item)) {
                    int take = Math.min(needed, item.getCount());
                    needed -= take;
                    item.shrink(take);
                    if (needed == 0)
                        break;
                }
            }
            if (needed > 0)
                return false;
        }
        return true;
    }

    @Override
    public boolean matches(NodeProcessRecipeInput input, Level level) {
        return matchesSubset(input.items());
    }

    @Override
    public ItemStack assemble(NodeProcessRecipeInput input) {
        return result.create();
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
    public @NotNull RecipeSerializer<@NotNull NodeProcessRecipe> getSerializer() {
        return RecipeSerializerRegistry.NODE_PROCESS_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<NodeProcessRecipeInput>> getType() {
        return RecipeTypeRegistry.NODE_PROCESS_RECIPE.get();
    }

    @Override
    public @NotNull PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public @NotNull RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
