package studio.fantasyit.ether_craft.factory;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.joml.Vector2i;
import studio.fantasyit.ether_craft.base.GraphLike;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.item.ProcessChipItem;
import studio.fantasyit.ether_craft.recipe.DelayedIngredient;
import studio.fantasyit.ether_craft.recipe.factory.EtherFactoryRecipeInput;
import studio.fantasyit.ether_craft.recipe.factory.PathNode;
import studio.fantasyit.ether_craft.register.ItemRegistry;
import studio.fantasyit.ether_craft.util.CollectionUtil;
import studio.fantasyit.ether_craft.util.SetUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class EtherProcessorRecipeUtil {
    private static final String DIRECT_INPUT = "DIRECT_INPUT";
    private static final TagKey<Item> TAG_CHIPS = ItemTags.create(Identifier.fromNamespaceAndPath("ether_craft", "chips"));


    public static class FactoryStructure {
        public List<EtherFactoryRecipeInput> recipes;
        public int[][] markMatrix;
        public int leakingSpeed;

        public FactoryStructure(int rs, int cs) {
            this.recipes = new ArrayList<>();
            this.markMatrix = new int[rs][cs];
            this.leakingSpeed = 0;
        }

    }

    /**
     * 获取输入树列表。输入是一个rows*cols的列表。前rows个为输入物品，后rows*cols为处理格子，行号优先，即第一行第一个，第二个...第二行，类推。
     *
     * @param input
     * @param rows
     * @param cols
     * @return
     */
    public static FactoryStructure processFactoryInput(int rows, int cols, Container input, EtherProcessWorkingChip[][] chipSlots) {
        FactoryStructure result = new FactoryStructure(rows, cols);
        List<ItemStack> inputs = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            inputs.add(input.getItem(i));
        }
        int[][] markMatrix = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (chipSlots[i][j] == null) {
                    markMatrix[i][j] = 0;
                } else if (!chipSlots[i][j].item.isEmpty()) {
                    markMatrix[i][j] = -1;
                } else {
                    markMatrix[i][j] = 100;
                }
            }
        }

        //A.标记所有无环且不经过最后一列的树
        for (int i = 0; i < rows; i++) {
            //从最后一行（即输出位置）的每一个输出格开始寻找可能的树结构
            boolean illegal = false;
            if (markTreeArea(markMatrix, cols - 1, i, -1, -1, i + 1)) {
                //标记了多个输出格的通路也是不合法的
                for (int j = 0; j < rows; j++) {
                    if (j != i && markMatrix[j][cols - 1] == i + 1) {
                        illegal = true;
                        break;
                    }
                }
                if (i == 0 || i == rows - 1)
                    illegal = true;
                if (!illegal) {
                    List<Integer> inputIds = new ArrayList<>();
                    List<Integer> processInputTrees = new ArrayList<>();
                    TreeLike<List<Integer>, List<ItemStack>> tree = new TreeLike<>(0, new ArrayList<>());
                    tree.addNode(1, new ArrayList<>());
                    tree.addEdge(0, 1, List.of(new ItemStack(ItemRegistry.DIRECT_INPUT_ITEM_CHIP.get())));
                    Set<EtherProcessWorkingChip> relevantComponents = new HashSet<>();
                    Set<PathNode> path = new HashSet<>();
                    scanForTrees(chipSlots, markMatrix, tree, inputIds, relevantComponents, path, processInputTrees, cols - 1, i, -1, -1, i + 1, 1, 0);

                    if (tree.getNodes().size() > 1) {
                        List<ItemStack> inputStacks = new ArrayList<>();
                        for (int inputId : inputIds) {
                            inputStacks.add(inputs.get(inputId));
                        }
                        result.recipes.add(new EtherFactoryRecipeInput(inputStacks, tree, inputIds, processInputTrees, i, relevantComponents, path));
                    }
                }
            } else {
                illegal = true;
            }
            if (illegal) {
                int inputCtn = 0;
                int outputCtn = 0;
                for (int j = 0; j < rows; j++) {
                    if (markMatrix[j][0] == i + 1) {
                        inputCtn++;
                    }
                    if (markMatrix[j][cols - 1] == i + 1) {
                        outputCtn++;
                    }
                }
                result.leakingSpeed += inputCtn * outputCtn * 10;
            }
        }
        result.markMatrix = markMatrix;
        return result;
    }

    final static int[][] DIRECTIONS = new int[][]{{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

    /**
     * 通路检查。返回值为当前通路是否为一颗合法树。当前通路的所有slot标记为markId。
     *
     * @param markMatrix
     * @param x
     * @param y
     * @param fromX
     * @param fromY
     * @param markId
     * @return
     */
    private static boolean markTreeArea(int[][] markMatrix,
                                        int x,
                                        int y,
                                        int fromX,
                                        int fromY,
                                        int markId) {
        if (markMatrix[y][x] != 0) {
            return false;
        }
        boolean result = true;
        markMatrix[y][x] = markId;
        for (int i = 0; i < 4; i++) {
            int x2 = x + DIRECTIONS[i][0];
            int y2 = y + DIRECTIONS[i][1];
            //忽略双线边构成的环
            if (x2 == fromX && y2 == fromY) {
                continue;
            }
            if (x2 >= 0 && x2 < markMatrix[0].length && y2 >= 0 && y2 < markMatrix.length) {
                if (markMatrix[y2][x2] == 0) {
                    if (!markTreeArea(markMatrix, x2, y2, x, y, markId)) result = false;
                } else if (markMatrix[y2][x2] != -1) {
                    //存在环
                    result = false;
                }
            }
        }
        return result;
    }

    /**
     * 生成合成树。该树表示了一种合成组合，需要在通路检查的结果上运行。
     *
     * @param scanMatrix
     * @param markMatrix
     * @param tree
     * @param inputIds
     * @param x
     * @param y
     * @param fromX
     * @param fromY
     * @param markId
     * @param parentId
     */
    private static void scanForTrees(EtherProcessWorkingChip[][] scanMatrix,
                                     int[][] markMatrix,
                                     TreeLike<List<Integer>, List<ItemStack>> tree,
                                     List<Integer> inputIds,
                                     Set<EtherProcessWorkingChip> relevantComponents,
                                     Set<PathNode> path,
                                     List<Integer> processInputTrees,
                                     int x,
                                     int y,
                                     int fromX,
                                     int fromY,
                                     int markId,
                                     int parentId,
                                     int depth) {
        if (x == -1) {
            inputIds.add(y);
            int id = tree.getMaxId() + 1;
            tree.addNode(id, new ArrayList<>());
            tree.addEdge(parentId, id, List.of(new ItemStack(ItemRegistry.DIRECT_INPUT_ITEM_CHIP.get())));
            processInputTrees.add(id);
            return;
        }
        //check chips around;
        List<ItemStack> chips = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int x2 = x + DIRECTIONS[i][0];
            int y2 = y + DIRECTIONS[i][1];
            if (x2 >= 0 && x2 < markMatrix[0].length && y2 >= 0 && y2 < markMatrix.length) {
                EtherProcessWorkingChip targetItem = scanMatrix[y2][x2];
                if (targetItem != null && !targetItem.item.isEmpty()) {
                    if (!ProcessChipItem.isSeparator(targetItem.item) && targetItem.effect.effectSide())
                        chips.add(targetItem.item);
                    relevantComponents.add(targetItem);
                }
            }
        }
        if (scanMatrix[y][x] != null && !scanMatrix[y][x].item.isEmpty()) {
            EtherProcessWorkingChip targetItem = scanMatrix[y][x];
            if (!ProcessChipItem.isSeparator(targetItem.item) && targetItem.effect.effectSelf())
                chips.add(targetItem.item);
            relevantComponents.add(targetItem);
        }

        // if Find
        if (chips.size() != 0) {
            int nxParentId = tree.getMaxId() + 1;
            tree.addNode(nxParentId, new ArrayList<>());
            tree.addEdge(parentId, nxParentId, chips);
            parentId = nxParentId;
        }

        Set<Vector2i> nexPositions = new HashSet<>();

        for (int i = 0; i < 4; i++) {
            int x2 = x + DIRECTIONS[i][0];
            int y2 = y + DIRECTIONS[i][1];
            if (x2 == fromX && y2 == fromY) {
                continue;
            }
            if (x2 >= -1 && x2 < markMatrix[0].length && y2 >= 0 && y2 < markMatrix.length) {
                if (x2 == -1 || markMatrix[y2][x2] == markId) {
                    EtherProcessWorkingChip targetItem = scanMatrix[y][x];
                    if (targetItem != null && !targetItem.item.isEmpty() && targetItem.effect.separate()) continue;
                    nexPositions.add(new Vector2i(x2, y2));
                    scanForTrees(scanMatrix, markMatrix, tree, inputIds, relevantComponents, path, processInputTrees, x2, y2, x, y, markId, parentId, depth + 1);
                }
            }
        }


        path.add(new PathNode(x, y, depth, nexPositions));
    }

    public static boolean isRecipeCompatible(TreeLike<Integer, List<DelayedIngredient>> recipeProcess, List<SizedIngredient> recipeInputs, EtherFactoryRecipeInput input) {
        if (input.inputs.size() != recipeInputs.size()) return false;

        Queue<TreeLike.TreeNode<List<Integer>, List<ItemStack>>> queue = new LinkedList<>();
        input.process.getNodes().forEach(node -> node.value.clear());
        input.process.getRoot().value.add(recipeProcess.getRoot().id);
        queue.add(input.process.getRoot());
        while (!queue.isEmpty()) {
            TreeLike.TreeNode<List<Integer>, List<ItemStack>> node = queue.poll();
            //第一步：获取当前可能处在配方树的节点位置
            node.value.forEach((id) -> {

                //2.1:获取当前实际位置向前传播的边（输入边）
                for (TreeLike.TreeEdge<List<Integer>, List<ItemStack>> edge : node.edges) {
                    //2.2:获取当前虚拟位置向前传播的边（配方边）
                    for (TreeLike.TreeEdge<Integer, List<DelayedIngredient>> recipeEdge : recipeProcess.getEdge(id)) {
                        /*
                        此时，我们获取到了输入边可能是的一条配方边，此时对这种可能性进行验证。
                        * */
                        if (edge.value.size() != recipeEdge.value.size()) {
                            continue;
                        }

                        AtomicBoolean allCompat = new AtomicBoolean(false);
                        CollectionUtil.fullPermutationIndex(edge.value, (indexes) -> {
                            boolean currentCompat = true;
                            for (int i = 0; i < indexes.length; i++) {
                                ItemStack itemStack = edge.value.get(indexes[i]);
                                if (!recipeEdge.value.get(i).toIngredient().test(itemStack)) {
                                    currentCompat = false;
                                }
                            }
                            if (currentCompat) {
                                allCompat.set(true);
                            }
                        });
                        if (allCompat.get()) {
                            //3.所有边都获得了对应匹配，则说明当前边可能是输入边对应的边，将当前边连接的配方虚拟节点加入输入树的目标节点
                            edge.node.value.add(recipeEdge.node.value);
                        }
                    }
                }

            });
            node.edges.forEach(edge -> queue.add(edge.node));
        }


        /*
        树遍历结束，每个输入结点上应该存储了对应的输入虚拟节点可能性。
         */
        List<Set<Integer>> possibleInputVids = new ArrayList<>(input.inputs.size());
        for (int i = 0; i < input.inputs.size(); i++) {
            int inputNodeId = input.inputTreeIds.get(i);
            possibleInputVids.add(Set.copyOf(input.process.getNode(inputNodeId).value));
        }
        /*
        对于每个物品，其可能的输入虚拟节点也可以计算
         */
        List<Set<Integer>> possibleRecipeVids = new ArrayList<>(recipeInputs.size());
        for (int i = 0; i < input.inputs.size(); i++) {
            possibleRecipeVids.add(new HashSet<>());
            for (int j = 0; j < recipeInputs.size(); j++) {
                if (recipeInputs.get(j).test(input.inputs.get(i))) {
                    possibleRecipeVids.get(i).add(j);
                }
            }
        }
        try {
            return SetUtil.setVeirfy(possibleInputVids, possibleRecipeVids);
        } catch (Exception ignore) {
            return false;
        }
    }

    public static int[] getToCostCountByInputAndIngredient(List<ItemStack> input, List<SizedIngredient> ingredient) {
        if (input.size() != ingredient.size())
            throw new IllegalArgumentException("input size != ingredient size");
        GraphLike<Integer> graph = new GraphLike<>();
        for (int i = 0; i < input.size() * 2; i++)
            graph.addNode(i);

        for (int i = 0; i < input.size(); i++) {
            for (int j = 0; j < input.size(); j++) {
                if (ingredient.get(j).test(input.get(i))) {
                    graph.addEdgeX(graph.getNode(i), graph.getNode(j + input.size()));
                }
            }
        }
        return SetUtil.biPartiteGraphMatchGetResult(graph, input.size(), input.size());
    }
}
