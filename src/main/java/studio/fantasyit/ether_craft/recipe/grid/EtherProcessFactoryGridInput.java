package studio.fantasyit.ether_craft.recipe.grid;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record EtherProcessFactoryGridInput(ItemStack target, int w, int h) implements RecipeInput {
    @Override
    public ItemStack getItem(int i) {
        if (i != 0)
            return ItemStack.EMPTY;
        return target;
    }

    @Override
    public int size() {
        return 1;
    }
}
