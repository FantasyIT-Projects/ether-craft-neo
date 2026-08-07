package studio.fantasyit.ether_craft.recipe.factory.multistep;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.factory.EtherProcessWorkingChip;
import studio.fantasyit.ether_craft.recipe.factory.PathNode;

import java.util.ArrayList;
import java.util.HashMap;
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
                                         Map<Integer, ItemStack> globalOutputTmpMapping,
                                         List<Integer> inputGlobalIds) {



    public ItemStack getGlobalItem(int idx) {
        if (globalInputMapping.containsKey(idx)) {
            return globalInputMapping.get(idx);
        }
        return globalOutputTmpMapping.get(idx);
    }

    /**
     * 复制本候选并刷新输入 ItemStack 引用为容器当前值。
     * 芯片布局不变时，processInputTrees/inputIds/relevantChip/workingPath/maxDepth/outputPositions
     * 均由布局决定，可直接共享；唯一会过期的是输入槽中的 ItemStack 对象引用（消耗/补充会替换槽位）。
     * globalOutputTmpMapping 必须新建，因为 getRecipe 会对它做 clear/put。
     * inputGlobalIds 与 inputIds 并行，即 globalInputMapping 的 key，用于直接重建输入映射。
     */
    public EtherFactoryMultiStepInput refreshedWith(Container container) {
        List<ItemStack> newInputs = new ArrayList<>(inputIds.size());
        Map<Integer, ItemStack> newInputMapping = new HashMap<>();
        for (int i = 0; i < inputIds.size(); i++) {
            ItemStack current = container.getItem(inputIds.get(i));
            newInputs.add(current);
            newInputMapping.put(inputGlobalIds.get(i), current);
        }
        return new EtherFactoryMultiStepInput(
                processInputTrees,
                inputIds,
                newInputs,
                outputI,
                relevantChip,
                workingPath,
                maxDepth,
                outputPositions,
                newInputMapping,
                new HashMap<>(),
                inputGlobalIds
        );
    }
}
