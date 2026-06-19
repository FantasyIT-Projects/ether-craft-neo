package studio.fantasyit.ether_craft.recipe.factory;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.base.TreeLike;

import java.util.List;
import java.util.Map;

public class EtherFactoryRecipeInput implements RecipeInput {
    //输入物品（ItemStack对象）
    public Map<Integer,ItemStack> inputs;
    // 处理树流程
    public TreeLike<List<Integer>, List<ItemStack>> process;

    public EtherFactoryRecipeInput(
            Map<Integer,ItemStack> inputs,
            TreeLike<List<Integer>, List<ItemStack>> process
    ) {
        this.inputs = inputs;
        this.process = process;
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
