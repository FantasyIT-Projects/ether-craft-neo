package studio.fantasyit.ether_craft.recipe.factory;

import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record EtherProcessRecipeJson(
        List<InputEntry> input,
        List<OutputEntry> output,
        List<ProcessEntry> process
) {
    public record InputEntry(String id, Ingredient item, @Nullable String next) {}
    public record OutputEntry(String id, Ingredient item) {}
    public record ProcessEntry(String id, List<Ingredient> item, @Nullable String next) {}
}