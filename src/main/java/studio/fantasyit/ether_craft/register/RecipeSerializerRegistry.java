package studio.fantasyit.ether_craft.register;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;

public class RecipeSerializerRegistry {
    static DeferredRegister<RecipeSerializer<?>> recipeSerializerIDeferredHolder = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, "ether_craft");
    public static final DeferredHolder<RecipeSerializer<?>, @NotNull RecipeSerializer<@NotNull EtherProcessFactoryRecipe>> ETHER_PROCESS_RECIPE_SERIALIZER =
            recipeSerializerIDeferredHolder.register("ether_process", () -> new RecipeSerializer<>(
                    EtherProcessFactoryRecipe.CODEC,
                    EtherProcessFactoryRecipe.STREAM_CODEC
            ));

    public static void register(IEventBus eventBus) {
        recipeSerializerIDeferredHolder.register(eventBus);
    }
}
