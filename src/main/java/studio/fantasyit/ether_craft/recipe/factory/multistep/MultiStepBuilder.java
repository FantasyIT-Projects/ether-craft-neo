package studio.fantasyit.ether_craft.recipe.factory.multistep;

import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.factory.EtherProcessWorkingChip;
import studio.fantasyit.ether_craft.item.ProcessChipItem;
import studio.fantasyit.ether_craft.recipe.factory.PathNode;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.*;

// 将单步流程树按 concat 芯片拆分为多步全局树。
// 每个 concat 边代表一段独立子流程，被提取为全局树中的独立 TreeRef 节点，
// 父步骤通过代理节点+DIRECT_CHIP边引用子步骤的输出。
public class MultiStepBuilder {

    private final TreeLike<List<Integer>, List<ItemStack>> tree;
    private final List<ItemStack> inputStacks;
    private final List<Integer> inputIds;
    private final List<Integer> inputTreeIds;
    private final Integer outputI;
    private final Set<EtherProcessWorkingChip> relevantChip;
    private final Set<PathNode> workingPath;
    private final Map<Integer, ItemStack> globalOutputMapping;
    TreeLike<EtherFactoryMultiStepInput.TreeRef, Integer> refTree;
    int maxGlobalIdx = 0;

    public MultiStepBuilder(
            TreeLike<List<Integer>, List<ItemStack>> tree,
            List<ItemStack> inputStacks,
            List<Integer> inputIds,
            List<Integer> inputTreeIds,
            Integer outputI,
            Set<EtherProcessWorkingChip> relevantChip,
            Set<PathNode> workingPath) {
        this.tree = tree;
        this.inputStacks = inputStacks;
        this.inputIds = inputIds;
        this.inputTreeIds = inputTreeIds;
        this.outputI = outputI;
        this.relevantChip = relevantChip;
        this.workingPath = workingPath;
        this.globalOutputMapping = new HashMap<>();
        for (int i = 0; i < inputStacks.size(); i++) {
            globalOutputMapping.put(inputTreeIds.get(i), inputStacks.get(i));
        }
        maxGlobalIdx = tree.getMaxId();
    }

    public EtherFactoryMultiStepInput getInput() {
        // 为最终输出分配全局ID
        int outputGlobalId = ++maxGlobalIdx;

        // 初始 inputMapping: 输入节点ID → 自身（已在 globalOutputMapping 中）
        Map<Integer, Integer> inputMapping = new HashMap<>();
        for (Integer inputId : inputTreeIds) {
            inputMapping.put(inputId, inputId);
        }

        // 将完整的步骤树作为全局树的根节点
        refTree = new TreeLike<>(0, new EtherFactoryMultiStepInput.TreeRef(tree, inputMapping, outputGlobalId));

        // 递归修剪concat边，将多段流程拆分为独立的子步骤
        trimScanAndTransformTree(refTree.getNode(0), tree.getRoot());

        int maxDepth = workingPath.stream().mapToInt(PathNode::depth).max().orElse(0);

        return new EtherFactoryMultiStepInput(refTree, inputIds, inputStacks, outputI, relevantChip, workingPath, maxDepth, globalOutputMapping, new HashMap<>());
    }

    public void trimScanAndTransformTree(
            TreeLike.TreeNode<EtherFactoryMultiStepInput.TreeRef, Integer> globalNode,
            TreeLike.TreeNode<List<Integer>, List<ItemStack>> mainTreeNode) {
        List<TreeLike.TreeEdge<List<Integer>, List<ItemStack>>> edgesCopy = new ArrayList<>(mainTreeNode.edges);
        for (TreeLike.TreeEdge<List<Integer>, List<ItemStack>> edge : edgesCopy) {
            if (edge.value.stream().anyMatch(ProcessChipItem::isConcat) && !edge.value.isEmpty()) {
                // [trim] 拷贝 concat 边后的子树为独立的新步骤树
                TreeLike<List<Integer>, List<ItemStack>> newTree = copySubTree(edge.node);

                EtherFactoryMultiStepInput.TreeRef currentRef = globalNode.value;
                TreeLike<List<Integer>, List<ItemStack>> currentTree = currentRef.tree();
                Map<Integer, Integer> parentInputMapping = currentRef.inputMapping();

                // 构建子步骤的 inputMapping：继承父 inputMapping 中位于子步骤树内的条目
                Map<Integer, Integer> childInputMapping = new HashMap<>();
                for (Map.Entry<Integer, Integer> entry : parentInputMapping.entrySet()) {
                    if (newTree.getNode(entry.getKey()) != null) {
                        childInputMapping.put(entry.getKey(), entry.getValue());
                    }
                }
                for (int key : childInputMapping.keySet()) {
                    parentInputMapping.remove(key);
                }

                // 为子步骤分配新的全局 output ID
                int newGlobalId = ++maxGlobalIdx;
                // 在全局树中注册子步骤节点
                int childRefNodeId = refTree.getMaxId() + 1;
                refTree.addNode(childRefNodeId, new EtherFactoryMultiStepInput.TreeRef(newTree, childInputMapping, newGlobalId));
                refTree.addEdge(globalNode.id, childRefNodeId, 0);

                // 从当前步骤树中删除 concat 边及整个子树
                mainTreeNode.edges.remove(edge);
                currentTree.removeSubTree(edge.node);

                // 用代理节点 + DIRECT_CHIP 边替换被删除的子树
                int proxyId = currentTree.getMaxId() + 1;
                currentTree.addNode(proxyId, new ArrayList<>());
                currentTree.addEdge(mainTreeNode.id, proxyId, List.of(new ItemStack(ItemRegistry.DIRECT_INPUT_ITEM_CHIP.get())));

                // 父步骤的 inputMapping 包含子步骤的输出
                parentInputMapping.put(proxyId, newGlobalId);

                // 递归处理子步骤内部的 concat 边
                trimScanAndTransformTree(refTree.getNode(childRefNodeId), newTree.getRoot());
            } else {
                // [scan] 非 concat 边，继续向下递归
                trimScanAndTransformTree(globalNode, edge.node);
            }
        }
    }

    // BFS遍历以root为根的子树，深拷贝到独立的 TreeLike 中
    private static TreeLike<List<Integer>, List<ItemStack>> copySubTree(
            TreeLike.TreeNode<List<Integer>, List<ItemStack>> root) {
        TreeLike<List<Integer>, List<ItemStack>> newTree = new TreeLike<>(0, new ArrayList<>());
        TreeLike.TreeNode<List<Integer>, List<ItemStack>> child1 = newTree.addNode(root.id, new ArrayList<>());
        newTree.addEdge(root.id, child1.id, List.of(new ItemStack(ItemRegistry.DIRECT_INPUT_ITEM_CHIP.get())));

        Set<Integer> visited = new HashSet<>();
        visited.add(root.id);
        List<TreeLike.TreeNode<List<Integer>, List<ItemStack>>> allNodes = new ArrayList<>();
        List<TreeLike.TreeEdge<List<Integer>, List<ItemStack>>> allEdges = new ArrayList<>();

        Queue<TreeLike.TreeNode<List<Integer>, List<ItemStack>>> queue = new LinkedList<>();
        queue.add(root);

        // 第一遍：收集所有节点和边
        while (!queue.isEmpty()) {
            TreeLike.TreeNode<List<Integer>, List<ItemStack>> node = queue.poll();
            allNodes.add(node);
            for (TreeLike.TreeEdge<List<Integer>, List<ItemStack>> edge : node.edges) {
                allEdges.add(edge);
                if (!visited.contains(edge.node.id)) {
                    visited.add(edge.node.id);
                    queue.add(edge.node);
                }
            }
        }

        // 第二遍：添加所有节点
        for (TreeLike.TreeNode<List<Integer>, List<ItemStack>> node : allNodes) {
            newTree.addNode(node.id, new ArrayList<>(node.value));
        }

        // 第三遍：添加所有边（value深拷贝）
        for (TreeLike.TreeEdge<List<Integer>, List<ItemStack>> edge : allEdges) {
            newTree.addEdge(edge.from.id, edge.node.id, new ArrayList<>(edge.value));
        }

        newTree.addEdge(0, root.id, List.of(new ItemStack(ItemRegistry.DIRECT_INPUT_ITEM_CHIP.get())));
        return newTree;
    }
}
