package studio.fantasyit.ether_craft.gametest;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.item.ProcessChipItem;
import studio.fantasyit.ether_craft.recipe.DelayedIngredient;
import studio.fantasyit.ether_craft.recipe.IngredientSerializer;
import studio.fantasyit.ether_craft.recipe.factory.EtherFactoryRecipeInput;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.recipe.factory.RecipeNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * GameTest 辅助工具：由配方自身推出一棵“镜像输入树”，
 * 使得在没有任何真实工厂布局的情况下也能断言 {@link EtherProcessFactoryRecipe#matches}。
 *
 * <p>镜像树与配方处理树同构（节点 id、边结构完全一致），边上的芯片由配方边的原料采样得到，
 * 输入槽物品由配方 input 采样得到，因此镜像输入必然应该匹配成功。
 */
public final class RecipeMirrorBuilder {
    private RecipeMirrorBuilder() {
    }

    /** 构造与配方匹配的输入：输入槽物品、输入节点 id、同构输入树。 */
    public static EtherFactoryRecipeInput mirrorOf(EtherProcessFactoryRecipe recipe) {
        List<ItemStack> inputs = new ArrayList<>(recipe.input.size());
        for (SizedIngredient ingredient : recipe.input) {
            inputs.add(sample(ingredient));
        }
        return new EtherFactoryRecipeInput(inputs, new ArrayList<>(recipe.inputNodeIds), mirrorTree(recipe));
    }

    /** 深拷贝配方处理树为输入树：节点 id 与边结构一致，边上的物品由配方边原料采样得到。 */
    public static TreeLike<List<Integer>, RecipeNode> mirrorTree(EtherProcessFactoryRecipe recipe) {
        TreeLike<Integer, List<DelayedIngredient>> src = recipe.process;
        TreeLike<List<Integer>, RecipeNode> mirror = new TreeLike<>(src.getRoot().id, new ArrayList<>());
        for (TreeLike.TreeNode<Integer, List<DelayedIngredient>> node : src.getNodes()) {
            if (!node.id.equals(src.getRoot().id)) {
                mirror.addNode(node.id, new ArrayList<>());
            }
        }
        for (TreeLike.TreeNode<Integer, List<DelayedIngredient>> node : src.getNodes()) {
            for (TreeLike.TreeEdge<Integer, List<DelayedIngredient>> edge : node.edges) {
                List<ItemStack> chips = new ArrayList<>(edge.value.size());
                for (DelayedIngredient ingredient : edge.value) {
                    chips.add(sample(ingredient));
                }
                mirror.addEdge(node.id, edge.node.id, RecipeNode.virtual(chips));
            }
        }
        return mirror;
    }

    /** 采样一个必定满足该原料的物品。芯片原料必须用严格组件匹配的芯片物品采样。 */
    public static ItemStack sample(DelayedIngredient ingredient) {
        var like = ingredient.ingredient().left().orElse(null);
        if (like instanceof IngredientSerializer.ChipRecord chip) {
            return ProcessChipItem.getStackFor(chip.id()).copyWithCount(ingredient.toIngredient().count());
        }
        return sample(ingredient.toIngredient());
    }

    /** 采样一个必定满足该原料（含数量）的物品。 */
    public static ItemStack sample(SizedIngredient ingredient) {
        // 组件原料（例如“携带芯片 id 的 process_chip”）必须连同组件一起构造
        if (ingredient.ingredient().getCustomIngredient() instanceof DataComponentIngredient components) {
            for (Holder<Item> holder : components.itemSet()) {
                ItemStack stack = new ItemStack(holder, ingredient.count());
                stack.applyComponents(components.components());
                if (ingredient.test(stack)) {
                    return stack;
                }
            }
        }
        for (Holder<Item> holder : ingredient.ingredient().items().toList()) {
            ItemStack stack = new ItemStack(holder, ingredient.count());
            if (ingredient.test(stack)) {
                return stack;
            }
        }
        throw new IllegalStateException("无法为该原料采样出满足条件的物品: " + ingredient);
    }

    /**
     * 从根开始检测树是否含环。配方 next 成环时会让匹配算法陷入无限 BFS，
     * 因此在调用 matches 前先做保护性检测。
     */
    public static boolean hasCycle(TreeLike<List<Integer>, RecipeNode> tree) {
        return hasCycle(tree.getRoot(), new HashSet<>(), new HashSet<>());
    }

    private static boolean hasCycle(TreeLike.TreeNode<List<Integer>, RecipeNode> node,
                                    Set<Integer> visiting,
                                    Set<Integer> visited) {
        if (visiting.contains(node.id)) {
            return true;
        }
        if (!visited.add(node.id)) {
            return false;
        }
        visiting.add(node.id);
        for (TreeLike.TreeEdge<List<Integer>, RecipeNode> edge : node.edges) {
            if (hasCycle(edge.node, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(node.id);
        return false;
    }
}
