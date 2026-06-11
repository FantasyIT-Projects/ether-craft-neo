package studio.fantasyit.ether_craft.recipe.factory.render.widget;

import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;

@FunctionalInterface
public interface NodeClickHandler<T> {
    void onClick(T data, TreeDiagramLayout.PositionedNode node, int button);
}
