package studio.fantasyit.ether_craft.recipe.factory;

import studio.fantasyit.ether_craft.base.TreeLike;

import java.util.List;
import java.util.Map;

public record TreeRef(
        TreeLike<List<Integer>, RecipeNode> tree,
        Map<Integer, Integer> inputMapping,
        int output
) {
}
