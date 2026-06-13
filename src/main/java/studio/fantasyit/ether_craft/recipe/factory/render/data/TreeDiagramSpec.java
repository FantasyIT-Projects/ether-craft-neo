package studio.fantasyit.ether_craft.recipe.factory.render.data;

import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeJson;

import java.util.ArrayList;
import java.util.List;

public record TreeDiagramSpec(
        List<TreeDiagramSpec.NodeSpec> nodes,
        TreeDiagramSpec.OutputSpec output,
        TreeDiagramSpec.LayoutConfig config
) {
    public record NodeSpec(
            String id,
            int subSlotCount,
            @Nullable String nextId,
            boolean isInput
    ) {
    }

    public record OutputSpec(int slotCount) {
    }

    public record LayoutConfig(
            int slotSize,
            int outputSlotSize,
            int chipGap,
            int chipOverlap,
            int viewWidth,
            int viewHeight,
            int padding,
            int nodeGap,
            int minSpacing
    ) {
        public static final LayoutConfig DEFAULT = new LayoutConfig(
                18, 22, 1, 0, 140, 90, 4, 15, 26
        );
    }

    public static TreeDiagramSpec fromJson(EtherProcessRecipeJson json) {
        List<NodeSpec> nodes = new ArrayList<>();
        for (var in : json.input()) {
            nodes.add(new NodeSpec(in.id(), 1, in.next(), true));
        }
        for (var proc : json.process()) {
            nodes.add(new NodeSpec(proc.id(), proc.item().size(), proc.next(), false));
        }
        return new TreeDiagramSpec(
                nodes,
                new OutputSpec(json.output().item().size()),
                LayoutConfig.DEFAULT
        );
    }
}
