package studio.fantasyit.ether_craft.recipe.factory.render.widget;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;

import java.util.List;

@FunctionalInterface
public interface NodeTooltipProvider<T> {
    @Nullable
    List<Component> getTooltip(T data, TreeDiagramLayout.PositionedNode node);
}
