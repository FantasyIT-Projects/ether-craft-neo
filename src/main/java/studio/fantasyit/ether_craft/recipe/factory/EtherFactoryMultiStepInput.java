package studio.fantasyit.ether_craft.recipe.factory;

import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.factory.EtherProcessWorkingChip;

import java.util.*;

public record EtherFactoryMultiStepInput(
        TreeLike<TreeRef, Integer> processInputTrees,
        List<Integer> inputIds,
        Integer outputI,
        Set<EtherProcessWorkingChip> relevantChip,
        Set<PathNode> workingPath,
        int maxDepth) {
    record TreeRef(
            TreeLike<List<Integer>, List<ItemStack>> tree,
            Map<Integer, Integer> inputMapping,
            Map<Integer, Integer> refStepMapping
    ) {
    }

    public EtherFactoryMultiStepInput(TreeLike<TreeLike<List<Integer>, List<ItemStack>>, Integer> processInputTrees,
                                      List<Integer> inputIds,
                                      Integer outputI,
                                      Set<EtherProcessWorkingChip> relevantChip,
                                      Set<PathNode> workingPath) {
        this(processInputTrees, inputIds, outputI, relevantChip, workingPath, workingPath.stream().mapToInt(PathNode::depth).max().orElse(0));
    }

    public static EtherFactoryMultiStepInput splitFromTree(
            TreeLike<List<Integer>, List<ItemStack>> tree,
            List<ItemStack> inputStacks,
            List<Integer> inputIds,
            List<Integer> inputTreeIds,
            Integer outputI,
            Set<EtherProcessWorkingChip> relevantChip,
            Set<PathNode> workingPath
    ) {
        TreeLike<TreeRef, Void> refTree = new TreeLike<>(0, null);
        TreeLike.TreeNode<TreeRef, Void> node = refTree.getRoot();
        Map<Integer, ItemStack> inputStackMapping = new HashMap<>();
        for (int i = 0; i < inputStacks.size(); i++) {
            inputStackMapping.put(inputTreeIds.get(i), inputStacks.get(i));
        }
        Map<Integer, Integer> refMapping = new HashMap<>();
        dfsScanTree(tree.getRoot(), node, inputStackMapping, refMapping);
        node.value = new TreeRef(node, )
    }

    private static void dfsScanTree(TreeLike.TreeNode<List<Integer>, List<ItemStack>> current, TreeLike.TreeNode<TreeRef, Void> parent, Map<Integer, ItemStack> inputTreeIdItems, Map<Integer, Integer> ref) {
        Set<TreeLike.TreeEdge<List<Integer>, List<ItemStack>>> toRemove = new HashSet<>();
        for (TreeLike.TreeEdge<List<Integer>, List<ItemStack>> edge : current.edges) {

        }
    }
}
