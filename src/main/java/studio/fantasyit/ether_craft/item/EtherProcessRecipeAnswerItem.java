package studio.fantasyit.ether_craft.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import studio.fantasyit.ether_craft.recipe.grid.EtherProcessFactoryGrid;
import studio.fantasyit.ether_craft.recipe.grid.EtherProcessFactoryGridInput;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;

import java.util.List;
import java.util.function.Function;

public class EtherProcessRecipeAnswerItem extends Item {
    public final int width;
    public final int height;

    public static Function<Identifier, EtherProcessRecipeAnswerItem> getConstructorWithShape(int width, int height) {
        return id -> new EtherProcessRecipeAnswerItem(id, width, height);
    }

    public EtherProcessRecipeAnswerItem(Identifier id, int width, int height) {
        super(new Properties().setId(ResourceKey.create(BuiltInRegistries.ITEM.key(), id)));
        this.width = width;
        this.height = height;
    }

    public EtherProcessFactoryGridInput getInput(ItemStack stack) {
        return new EtherProcessFactoryGridInput(stack, width, height);
    }

    public List<EtherProcessFactoryGrid> getCompatibleGrids(ItemStack stack, ServerLevel level) {
        EtherProcessFactoryGridInput input = getInput(stack);
        RecipeManager recipeAccess = level.getServer().getRecipeManager();
        return recipeAccess.getRecipes()
                .stream().filter(r -> r.value().getType() == RecipeTypeRegistry.ETHER_PROCESS_FACTORY_GRID.get())
                .map(r -> (EtherProcessFactoryGrid) r.value())
                .filter(r -> r.matches(input, level)).toList();
    }
}
