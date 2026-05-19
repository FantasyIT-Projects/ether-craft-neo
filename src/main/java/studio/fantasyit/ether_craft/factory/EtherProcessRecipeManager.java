package studio.fantasyit.ether_craft.factory;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import studio.fantasyit.ether_craft.factory.special.ExtraFurnaceRecipe;
import studio.fantasyit.ether_craft.recipe.factory.EtherFactoryRecipeInput;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class EtherProcessRecipeManager {
    public record ExtraRecipe(Identifier categoryId, Identifier id, EtherProcessFactoryRecipe recipe) {
        public static StreamCodec<RegistryFriendlyByteBuf, ExtraRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Identifier.STREAM_CODEC,
                        ExtraRecipe::categoryId,
                        Identifier.STREAM_CODEC,
                        ExtraRecipe::id,
                        EtherProcessFactoryRecipe.STREAM_CODEC,
                        ExtraRecipe::recipe,
                        ExtraRecipe::new
                );
    }

    public static List<ExtraRecipe> extraRecipes = new ArrayList<>();
    public static List<Function<RecipeManager, List<ExtraRecipe>>> extraRecipeProviders = new ArrayList<>();

    public static void registerExtraRecipeProvider(Function<RecipeManager, List<ExtraRecipe>> provider) {
        extraRecipeProviders.add(provider);
    }

    public static void collectProvider() {
        extraRecipeProviders.add(new ExtraFurnaceRecipe());
    }

    public static void onReload(RecipeManager manager) {
        extraRecipes.clear();
        for (Function<RecipeManager, List<ExtraRecipe>> provider : extraRecipeProviders) {
            extraRecipes.addAll(provider.apply(manager));
        }
        //TODO sync
    }
    //TODO from network

    public static Optional<EtherProcessFactoryRecipe> getRecipe(Level level, RecipeManager manager, EtherFactoryRecipeInput input) {
        Optional<RecipeHolder<EtherProcessFactoryRecipe>> o = manager.getRecipeFor(RecipeTypeRegistry.ETHER_PROCESS_FACTORY_RECIPE.get(), input, level);
        if (o.isPresent())
            return Optional.of(o.get().value());
        for (ExtraRecipe extraRecipe : extraRecipes) {
            if (extraRecipe.recipe.matches(input, level)) {
                return Optional.of(extraRecipe.recipe);
            }
        }
        return Optional.empty();
    }
}
