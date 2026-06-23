package studio.fantasyit.ether_craft.recipe.factory;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.apache.commons.lang3.mutable.MutableBoolean;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.factory.ExtraRecipeProvider;
import studio.fantasyit.ether_craft.factory.special.ExtraFurnaceRecipe;
import studio.fantasyit.ether_craft.recipe.factory.multistep.EtherFactoryMultiStepInput;
import studio.fantasyit.ether_craft.recipe.factory.multistep.MultiStepMatchIO;
import studio.fantasyit.ether_craft.recipe.factory.multistep.MultiStepMatchIOTemp;
import studio.fantasyit.ether_craft.recipe.factory.multistep.TreeRef;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;
import studio.fantasyit.ether_craft.util.MathUtil;

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
        TreeLike<EtherProcessFactoryRecipe, Void> keyTree = new TreeLike<>(0, null);
        MultiStepMatchIOTemp result = new MultiStepMatchIOTemp();
        MutableBoolean isFail = new MutableBoolean(false);
        input.globalOutputTmpMapping().clear();
        getRecipeRecurse(level, manager, input.processInputTrees().getRoot(), input, result, isFail, keyTree, keyTree.getRoot().id, true);
        if (isFail.booleanValue()) {
            return Optional.of(result.getFailed(input,keyTree));
        }
        return Optional.of(result.propagateMultipliersAndGetImmutable(input, keyTree));
    }

    public static void getRecipeRecurse(Level level,
                                        RecipeManager manager,
                                        TreeLike.TreeNode<TreeRef, Integer> node,
                                        EtherFactoryMultiStepInput multiStepInput,
                                        MultiStepMatchIOTemp io,
                                        MutableBoolean isFail,
                                        TreeLike<EtherProcessFactoryRecipe, Void> keyTree,
                                        int keyTreeNode,
                                        boolean isTop) {
        for (TreeLike.TreeEdge<TreeRef, Integer> e : node.edges) {
            TreeLike.TreeNode<EtherProcessFactoryRecipe, Void> nxtNode = keyTree.addNode(keyTree.getMaxId() + 1, null);
            keyTree.addEdge(keyTreeNode, nxtNode.id, null);
            getRecipeRecurse(level, manager, e.node, multiStepInput, io, isFail, keyTree, nxtNode.id, false);
            if (isFail.booleanValue()) {
                return;
            }
        }
        TreeRef value = node.value;

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
            isFail.setTrue();
            return;
        }
        keyTree.getNode(keyTreeNode).value = recipe.get();
        int[] toCostCountByInputAndIngredient = EtherProcessorRecipeUtil.getToCostCountByInputAndIngredient(inputStacks, recipe.get().input);
        int multipler = 1;
        for (int i = 0; i < toCostCountByInputAndIngredient.length; i++) {
            if (toCostCountByInputAndIngredient[i] == -1) {
                isFail.setTrue();
                return;
            }
            SizedIngredient recipeIngredient = recipe.get().input.get(toCostCountByInputAndIngredient[i]);
            if (isInput.get(i)) {
                io.addInput(recipeIngredient, node.value.output());
            } else {
                multipler = MathUtil.findLCM(MathUtil.findLCM(inputStacks.get(i).count(), recipeIngredient.count()) / recipeIngredient.count(), multipler);
            }
        }

        for (int i = 0; i < toCostCountByInputAndIngredient.length; i++) {
            SizedIngredient recipeIngredient = recipe.get().input.get(toCostCountByInputAndIngredient[i]);
            if (!isInput.get(i)) {
                int count = multipler * recipeIngredient.count() / inputStacks.get(i).count();
                io.setNodeMultiplierOuter(inputTreeIds.get(i), count);
            }
        }
        io.setNodeMultiplierInner(value.output(), multipler);


        if (isTop) {
            recipe.get().output.stream().map(ItemStackTemplate::create).forEach(t -> io.addOutput(t, node.value.output()));
        } else {
            List<ItemStackTemplate> output = recipe.get().output;
            if (output.size() != 1) {
                for (int i = 1; i < output.size(); i++) {
                    io.addExtraOutput(output.get(i).create(), node.value.output());
                }
            }
            int outputId = value.output();
            multiStepInput.globalOutputTmpMapping().put(outputId, output.getFirst().create());
        }
    }
}
