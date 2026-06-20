package studio.fantasyit.ether_craft.factory;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.mutable.MutableBoolean;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.factory.special.ExtraFurnaceRecipe;
import studio.fantasyit.ether_craft.recipe.factory.EtherFactoryRecipeInput;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.recipe.factory.multistep.EtherFactoryMultiStepInput;
import studio.fantasyit.ether_craft.recipe.factory.multistep.MultiStepMatchIO;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public static List<ExtraRecipeProvider> extraRecipeProviders = new ArrayList<>();

    public static void registerExtraRecipeProvider(ExtraRecipeProvider provider) {
        extraRecipeProviders.add(provider);
    }

    public static ExtraRecipeProvider getProviderFor(Identifier categoryId) {
        for (ExtraRecipeProvider p : extraRecipeProviders) {
            if (p.getCategoryId().equals(categoryId)) {
                return p;
            }
        }
        return null;
    }

    public static void collectProvider() {
        extraRecipeProviders.add(new ExtraFurnaceRecipe());
    }

    public static void onReload(RecipeManager manager) {
        extraRecipes = new ArrayList<>();
        for (ExtraRecipeProvider provider : extraRecipeProviders) {
            extraRecipes.addAll(provider.generate(manager));
        }
    }


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


    public static Optional<MultiStepMatchIO> getRecipe(Level level, RecipeManager manager, EtherFactoryMultiStepInput input) {
        MultiStepMatchIO result = new MultiStepMatchIO(new ArrayList<>(), new ArrayList<>());
        MutableBoolean isFail = new MutableBoolean(false);
        input.globalOutputTmpMapping().clear();
        getRecipeRecurse(level, manager, input.processInputTrees().getRoot(), input, result, isFail, true);
        if (isFail.booleanValue()) {
            return Optional.empty();
        }
        return Optional.of(result);
    }

    public static void getRecipeRecurse(Level level, RecipeManager manager, TreeLike.TreeNode<EtherFactoryMultiStepInput.TreeRef, Integer> node, EtherFactoryMultiStepInput multiStepInput, MultiStepMatchIO io, MutableBoolean isFail, boolean isTop) {
        for (TreeLike.TreeEdge<EtherFactoryMultiStepInput.TreeRef, Integer> e : node.edges) {
            getRecipeRecurse(level, manager, e.node, multiStepInput, io, isFail, false);
            if (isFail.booleanValue()) {
                return;
            }
        }
        EtherFactoryMultiStepInput.TreeRef value = node.value;

        List<Integer> inputTreeIds = new ArrayList<>();
        List<ItemStack> inputStacks = new ArrayList<>();
        List<Boolean> isInput = new ArrayList<>();

        for (Map.Entry<Integer, Integer> mapping : node.value.inputMapping().entrySet()) {
            inputTreeIds.add(mapping.getKey());
            inputStacks.add(multiStepInput.getGlobalItem(mapping.getValue()));
            isInput.add(multiStepInput.globalInputMapping().containsKey(mapping.getKey()));
        }

        EtherFactoryRecipeInput etherFactoryRecipeInput = new EtherFactoryRecipeInput(inputStacks, inputTreeIds, value.tree());
        Optional<EtherProcessFactoryRecipe> recipe = getRecipe(level, manager, etherFactoryRecipeInput);
        if (recipe.isEmpty()) {
            isFail.setFalse();
            return;
        }
        List<ItemStackTemplate> output = recipe.get().output;
        if (output.size() != 1 && !isTop) {
            isFail.setFalse();
            return;
        }
        int outputId = value.output();
        multiStepInput.globalOutputTmpMapping().put(outputId, output.getFirst().create());
        int[] toCostCountByInputAndIngredient = EtherProcessorRecipeUtil.getToCostCountByInputAndIngredient(inputStacks, recipe.get().input);
        for (int i = 0; i < toCostCountByInputAndIngredient.length; i++) {
            if (toCostCountByInputAndIngredient[i] == -1) {
                isFail.setFalse();
                return;
            }
            if (isInput.get(i)) {
                io.inputs().add(recipe.get().input.get(toCostCountByInputAndIngredient[i]));
            }
        }
        if (isTop) {
            recipe.get().output.stream().map(ItemStackTemplate::create).forEach(io.outputs()::add);
        }
    }
}
