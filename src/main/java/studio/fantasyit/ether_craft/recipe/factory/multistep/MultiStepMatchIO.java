package studio.fantasyit.ether_craft.recipe.factory.multistep;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

public record MultiStepMatchIO(List<SizedIngredient> inputs, List<ItemStack> outputs) {
}
