package studio.fantasyit.ether_craft.recipe.factory.render.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;

@FunctionalInterface
public interface NodeRenderer<T> {
    void render(GuiGraphicsExtractor g, TreeDiagramLayout.PositionedNode node, T data,
                double mouseWorldX, double mouseWorldY);
}
