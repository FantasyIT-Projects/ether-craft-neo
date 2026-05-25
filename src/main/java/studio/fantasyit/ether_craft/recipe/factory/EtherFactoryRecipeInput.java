package studio.fantasyit.ether_craft.recipe.factory;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.factory.EtherProcessWorkingChip;

import java.util.List;
import java.util.Set;

public class EtherFactoryRecipeInput implements RecipeInput {
    //输入物品（ItemStack对象）
    public List<ItemStack> inputs;
    // 处理树流程
    public TreeLike<List<Integer>, List<ItemStack>> process;
    //输入物品（在输入树中的ID）
    public List<Integer> inputTreeIds;
    //输入物品（在输入序列中的ID）
    public List<Integer> inputIds;
    //输出物品（在输出序列中的ID）
    public Integer outputId;
    //相关的组件
    public Set<EtherProcessWorkingChip> relevantChips;
    //通路
    public Set<PathNode> workingPath;
    public int maxDepth;

    public EtherFactoryRecipeInput(List<ItemStack> inputs,
                                   TreeLike<List<Integer>, List<ItemStack>> process,
                                   List<Integer> inputIds,
                                   List<Integer> inputTreeIds,
                                   Integer outputId,
                                   Set<EtherProcessWorkingChip> relevantChips,
                                   Set<PathNode> workingPath
    ) {
        this.inputs = inputs;
        this.process = process;
        this.inputTreeIds = inputTreeIds;
        this.inputIds = inputIds;
        this.outputId = outputId;
        this.relevantChips = relevantChips;
        this.workingPath = workingPath;
        this.maxDepth = workingPath.stream().mapToInt(PathNode::depth).max().orElse(0);
    }

    @Override
    public @NotNull ItemStack getItem(int i) {
        return inputs.get(i);
    }

    @Override
    public int size() {
        return inputs.size();
    }
}
