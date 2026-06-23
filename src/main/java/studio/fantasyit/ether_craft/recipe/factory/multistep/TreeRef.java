package studio.fantasyit.ether_craft.recipe.factory.multistep;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.recipe.factory.RecipeNode;

import java.util.List;
import java.util.Map;

public record TreeRef(
        TreeLike<List<Integer>, RecipeNode> tree,
        Map<Integer, Integer> inputMapping,
        int output
) {
}
