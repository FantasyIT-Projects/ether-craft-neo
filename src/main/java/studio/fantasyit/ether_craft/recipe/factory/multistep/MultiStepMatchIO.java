package studio.fantasyit.ether_craft.recipe.factory.multistep;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;

import java.util.List;
import java.util.Objects;

public record MultiStepMatchIO(List<SizedIngredient> inputs,
                               List<ItemStack> outputs,
                               TreeLike<EtherProcessFactoryRecipe, Void> key,
                               int maxStepMultiplier) {
    public boolean isSameTo(MultiStepMatchIO other) {
        if (other == null) return false;
        return isSame(key.getRoot(), other.key.getRoot());
    }

    private static boolean isSame(TreeLike.TreeNode<EtherProcessFactoryRecipe, Void> n1, TreeLike.TreeNode<EtherProcessFactoryRecipe, Void> n2) {
        if (!Objects.equals(n1.value, n2.value)) {
            return false;
        }
        if (n1.edges.size() != n2.edges.size())
            return false;
        for (int i = 0; i < n1.edges.size(); i++) {
            if (!isSame(n1.edges.get(i).node, n2.edges.get(i).node))
                return false;
        }
        return true;
    }
}
