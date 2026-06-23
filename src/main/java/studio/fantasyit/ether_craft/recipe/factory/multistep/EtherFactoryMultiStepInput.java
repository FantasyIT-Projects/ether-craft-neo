package studio.fantasyit.ether_craft.recipe.factory.multistep;

import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.factory.EtherProcessWorkingChip;
import studio.fantasyit.ether_craft.recipe.factory.PathNode;
import studio.fantasyit.ether_craft.recipe.factory.RecipeNode;
import studio.fantasyit.ether_craft.recipe.factory.TreeRef;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record EtherFactoryMultiStepInput(TreeLike<TreeRef, Integer> processInputTrees,
                                         List<Integer> inputIds,
                                         List<ItemStack> inputs,
                                         Integer outputI,
                                         Set<EtherProcessWorkingChip> relevantChip,
                                         Set<PathNode> workingPath,
                                         int maxDepth,
                                         Map<Vector2i, Integer> outputPositions,
                                         Map<Integer, ItemStack> globalInputMapping,
                                         Map<Integer, ItemStack> globalOutputTmpMapping) {



    public ItemStack getGlobalItem(int idx) {
        if (globalInputMapping.containsKey(idx)) {
            return globalInputMapping.get(idx);
        }
        return globalOutputTmpMapping.get(idx);
    }
}
