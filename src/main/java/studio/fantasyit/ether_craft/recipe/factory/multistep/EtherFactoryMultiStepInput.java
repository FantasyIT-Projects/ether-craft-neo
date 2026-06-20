package studio.fantasyit.ether_craft.recipe.factory.multistep;

import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.factory.EtherProcessWorkingChip;
import studio.fantasyit.ether_craft.recipe.factory.PathNode;

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
                                         Map<Integer, ItemStack> globalInputMapping,
                                         Map<Integer, ItemStack> globalOutputTmpMapping) {

    public record TreeRef(
            TreeLike<List<Integer>, List<ItemStack>> tree,
            Map<Integer, Integer> inputMapping,
            int output
    ) {
    }

    public ItemStack getGlobalItem(int idx) {
        if (globalInputMapping.containsKey(idx)) {
            return globalInputMapping.get(idx);
        }
        return globalOutputTmpMapping.get(idx);
    }
}
