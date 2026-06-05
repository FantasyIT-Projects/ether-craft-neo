package studio.fantasyit.ether_craft.recipe.plating;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PlatingRecipeInput implements RecipeInput {
    public final List<ItemStack> items;

    public PlatingRecipeInput(List<ItemStack> items) {
        this.items = items;
    }

    @Override
    public @NotNull ItemStack getItem(int i) {
        return items.get(i);
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }
}