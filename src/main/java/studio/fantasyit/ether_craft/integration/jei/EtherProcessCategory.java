package studio.fantasyit.ether_craft.integration.jei;

import com.mojang.serialization.Codec;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.ICodecHelper;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeJson;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.*;

public class EtherProcessCategory implements IRecipeCategory<EtherProcessFactoryRecipe> {
    private static final int SLOT_SIZE = 18;
    private static final int CHIP_GAP = 1;
    private static final int NODE_GAP = 4;
    private static final int LEVEL_GAP = 44;
    private static final int PADDING = 4;
    private static final int WIDTH = 280;
    private static final int HEIGHT = 100;
    private static final int LINE_COLOR = 0xFFAAAAAA;

    private final IDrawable icon;

    public EtherProcessCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_1.get())
        );
    }

    @Override
    public IRecipeType<EtherProcessFactoryRecipe> getRecipeType() {
        return JEIPlugin.ETHER_PROCESS_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.ether_craft.ether_process");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EtherProcessFactoryRecipe recipe, IFocusGroup focuses) {
        TreeLayout layout = TreeLayout.compute(recipe.json);

        for (TreeLayout.Entry e : layout.inputs) {
            builder.addInputSlot(e.x, e.y)
                    .add(e.ingredient)
                    .setStandardSlotBackground();
        }
        for (TreeLayout.ChipEntry e : layout.chips) {
            builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, e.x, e.y)
                    .add(e.ingredient)
                    .setStandardSlotBackground();
        }
        int outX = layout.outputX;
        for (var item : recipe.json.output().item()) {
            builder.addOutputSlot(outX, layout.outputY)
                    .add(item)
                    .setOutputSlotBackground();
            outX += SLOT_SIZE + 2;
        }
    }

    @Override
    public void draw(EtherProcessFactoryRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        TreeLayout layout = TreeLayout.compute(recipe.json);
        for (TreeLayout.Edge edge : layout.edges) {
            drawLine(graphics, edge.fromX, edge.fromY, edge.toX, edge.toY);
        }
    }

    private static void drawLine(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2) {
        int midX = (x1 + x2) / 2;
        hLine(graphics, x1, midX, y1);
        vLine(graphics, midX, y1, y2);
        hLine(graphics, midX, x2, y2);
    }

    private static void hLine(GuiGraphicsExtractor graphics, int x1, int x2, int y) {
        int from = Math.min(x1, x2);
        int to = Math.max(x1, x2);
        graphics.fill(from, y, to + 1, y + 1, LINE_COLOR);
    }

    private static void vLine(GuiGraphicsExtractor graphics, int x, int y1, int y2) {
        int from = Math.min(y1, y2);
        int to = Math.max(y1, y2);
        graphics.fill(x, from, x + 1, to + 1, LINE_COLOR);
    }

    @Override
    public Codec<EtherProcessFactoryRecipe> getCodec(ICodecHelper codecHelper, IRecipeManager recipeManager) {
        return EtherProcessFactoryRecipe.CODEC.codec();
    }

    @Override
    public @Nullable Identifier getIdentifier(EtherProcessFactoryRecipe recipe) {
        return null;
    }

    private static class TreeLayout {
        record Entry(String id, int x, int y, Ingredient ingredient) {}
        record ChipEntry(String parentId, int x, int y, Ingredient ingredient) {}
        record Edge(int fromX, int fromY, int toX, int toY) {}

        final List<Entry> inputs = new ArrayList<>();
        final List<ChipEntry> chips = new ArrayList<>();
        final List<Edge> edges = new ArrayList<>();
        int outputX, outputY;

        static TreeLayout compute(EtherProcessRecipeJson json) {
            TreeLayout layout = new TreeLayout();

            Set<String> allIds = new HashSet<>();
            Map<String, String> nextMap = new HashMap<>();
            Map<String, Boolean> isInput = new HashMap<>();

            for (var in : json.input()) {
                allIds.add(in.id());
                nextMap.put(in.id(), in.next());
                isInput.put(in.id(), true);
            }
            for (var proc : json.process()) {
                allIds.add(proc.id());
                nextMap.put(proc.id(), proc.next());
                isInput.put(proc.id(), false);
            }

            Map<String, Integer> levels = new HashMap<>();
            for (String id : allIds) {
                computeLevel(id, nextMap, allIds, levels);
            }
            int maxLevel = levels.values().stream().max(Integer::compare).orElse(0);

            Map<Integer, List<String>> byLevel = new TreeMap<>();
            for (var e : levels.entrySet()) {
                byLevel.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
            }

            Map<String, Integer> nodeX = new HashMap<>();
            Map<String, Integer> nodeY = new HashMap<>();
            Map<String, Integer> nodeH = new HashMap<>();

            int maxTotalH = 0;
            for (var entry : byLevel.entrySet()) {
                int level = entry.getKey();
                int colX = PADDING + (maxLevel - level) * LEVEL_GAP;
                List<String> ids = entry.getValue();

                int totalH = 0;
                for (String id : ids) {
                    int h;
                    if (isInput.get(id)) {
                        h = SLOT_SIZE;
                    } else {
                        int cnt = chipCount(json, id);
                        h = cnt * SLOT_SIZE + (cnt - 1) * CHIP_GAP;
                    }
                    nodeH.put(id, h);
                    totalH += h + NODE_GAP;
                }
                totalH -= NODE_GAP;
                maxTotalH = Math.max(maxTotalH, totalH);

                int curY = PADDING + (HEIGHT - 2 * PADDING - totalH) / 2;
                for (String id : ids) {
                    nodeX.put(id, colX);
                    nodeY.put(id, curY);
                    curY += nodeH.get(id) + NODE_GAP;
                }
            }

            int outCount = json.output().item().size();
            int outWidth = outCount * SLOT_SIZE + (outCount - 1) * 2;
            layout.outputX = PADDING + maxLevel * LEVEL_GAP + (LEVEL_GAP - outWidth) / 2;
            layout.outputY = (HEIGHT - SLOT_SIZE) / 2;

            for (var in : json.input()) {
                layout.inputs.add(new Entry(in.id(), nodeX.get(in.id()), nodeY.get(in.id()), in.item().ingredient()));
            }
            for (var proc : json.process()) {
                int cx = nodeX.get(proc.id());
                int cy = nodeY.get(proc.id());
                for (var sized : proc.item()) {
                    layout.chips.add(new ChipEntry(proc.id(), cx, cy, sized.ingredient()));
                    cy += SLOT_SIZE + CHIP_GAP;
                }
            }

            for (var in : json.input()) {
                int fx = nodeX.get(in.id()) + SLOT_SIZE;
                int fy = nodeY.get(in.id()) + SLOT_SIZE / 2;
                String next = in.next();
                int tx, ty;
                if (next != null && allIds.contains(next)) {
                    tx = nodeX.get(next);
                    ty = nodeY.get(next) + nodeMidY(isInput.get(next), nodeH.get(next));
                } else {
                    tx = layout.outputX;
                    ty = layout.outputY + SLOT_SIZE / 2;
                }
                layout.edges.add(new Edge(fx, fy, tx, ty));
            }
            for (var proc : json.process()) {
                int fx = nodeX.get(proc.id()) + SLOT_SIZE;
                int fy = nodeY.get(proc.id()) + nodeMidY(false, nodeH.get(proc.id()));
                String next = proc.next();
                int tx, ty;
                if (next != null && allIds.contains(next)) {
                    tx = nodeX.get(next);
                    ty = nodeY.get(next) + nodeMidY(isInput.get(next), nodeH.get(next));
                } else {
                    tx = layout.outputX;
                    ty = layout.outputY + SLOT_SIZE / 2;
                }
                layout.edges.add(new Edge(fx, fy, tx, ty));
            }

            return layout;
        }

        private static int nodeMidY(boolean isInput, int h) {
            return isInput ? SLOT_SIZE / 2 : h / 2;
        }

        private static int chipCount(EtherProcessRecipeJson json, String id) {
            for (var p : json.process()) {
                if (p.id().equals(id)) return p.item().size();
            }
            return 1;
        }

        private static int computeLevel(String id, Map<String, String> nextMap,
                                         Set<String> allIds, Map<String, Integer> levels) {
            if (levels.containsKey(id)) return levels.get(id);
            String next = nextMap.getOrDefault(id, null);
            int lvl;
            if (next == null || !allIds.contains(next)) {
                lvl = 1;
            } else {
                lvl = computeLevel(next, nextMap, allIds, levels) + 1;
            }
            levels.put(id, lvl);
            return lvl;
        }
    }
}
