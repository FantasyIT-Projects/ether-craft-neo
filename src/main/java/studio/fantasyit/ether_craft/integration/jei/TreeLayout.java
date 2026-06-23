package studio.fantasyit.ether_craft.integration.jei;

import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.DisplayContentsFactory;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeJson;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramSpec;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeLayoutCalculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeLayout {
    static final int SLOT_SIZE = 18;
    static final int SLOT_SIZE_OUTPUT = 22;
    static final int CHIP_GAP = 1;
    static final int WIDTH = 280;
    static final int HEIGHT = 110;
    static final int WIDTH_SMALL = 140;
    static final int HEIGHT_SMALL = 90;

    record Entry(String id, int x, int y, SizedIngredient ingredient) {
    }

    record ChipEntry(String parentId, int x, int y, SizedIngredient ingredient) {
    }

    record Edge(int fromX, int fromY, int toX, int toY) {
    }

    final List<Entry> inputs = new ArrayList<>();
    final List<ChipEntry> chips = new ArrayList<>();
    final List<Edge> edges = new ArrayList<>();
    int outputX, outputY;
    int canvasWidth;
    int canvasHeight;

    static TreeLayout compute(EtherProcessRecipeJson json) {
        TreeDiagramSpec spec = TreeDiagramSpec.fromJson(json);
        TreeDiagramLayout computed = TreeLayoutCalculator.compute(spec);

        TreeLayout layout = new TreeLayout();
        layout.canvasWidth = computed.canvasWidth;
        layout.canvasHeight = computed.canvasHeight;
        layout.outputX = computed.output.x();
        layout.outputY = computed.output.y();

        Map<String, SizedIngredient> inputIngredient = new HashMap<>();
        for (var in : json.input()) {
            inputIngredient.put(in.id(), in.item());
        }
        for (var pn : computed.nodes) {
            SizedIngredient ing = inputIngredient.get(pn.id());
            if (ing != null) {
                layout.inputs.add(new Entry(pn.id(), pn.x(), pn.y(), ing));
            }
        }

        for (var proc : json.process()) {
            String id = proc.id();
            var positioned = computed.nodes.stream()
                    .filter(n -> n.id().equals(id)).findFirst().orElse(null);
            if (positioned == null) continue;
            int cx = positioned.x();
            int cy = positioned.y();
            for (var sized : proc.item()) {
                layout.chips.add(new ChipEntry(id, cx, cy, sized));
                cy += SLOT_SIZE + CHIP_GAP;
            }
        }

        for (var edge : computed.edges) {
            layout.edges.add(new Edge(edge.fromX(), edge.fromY(), edge.toX(), edge.toY()));
        }

        return layout;
    }

    public static List<ItemStack> resolveSizedIngredient(SizedIngredient sized, ContextMap context) {
        return sized.ingredient().display()
                .resolve(context, (DisplayContentsFactory.ForStacks<ItemStack>)
                        stack -> stack.copyWithCount(sized.count()))
                .toList();
    }

}
