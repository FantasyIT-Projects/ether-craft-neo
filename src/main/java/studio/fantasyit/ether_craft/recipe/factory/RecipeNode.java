package studio.fantasyit.ether_craft.recipe.factory;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record RecipeNode(int x, int y, List<ItemStack> input) {
    public RecipeNode(RecipeNode value) {
        this(value.x, value.y, value.input);
    }

    public static RecipeNode virtual(List<ItemStack> itemStacks) {
        return new RecipeNode(-1, -1, itemStacks);
    }
}
