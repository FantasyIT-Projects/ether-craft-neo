package studio.fantasyit.ether_craft.register;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.recipe.node.NodeProcessRecipe;

public class RecipeTypeRegistry {
    static DeferredRegister<RecipeType<?>> recipeTypes = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, "ether_craft");
    public static final DeferredHolder<RecipeType<?>, @NotNull RecipeType<@NotNull EtherProcessFactoryRecipe>> ETHER_PROCESS_FACTORY_RECIPE = recipeTypes.register("ether_process", () -> new RecipeType<>() {
    });
    public static final DeferredHolder<RecipeType<?>, @NotNull RecipeType<@NotNull NodeProcessRecipe>> NODE_PROCESS_RECIPE = recipeTypes.register("node_process", () -> new RecipeType<>() {
    });

    public static void register(IEventBus eventBus) {
        recipeTypes.register(eventBus);
    }
}
