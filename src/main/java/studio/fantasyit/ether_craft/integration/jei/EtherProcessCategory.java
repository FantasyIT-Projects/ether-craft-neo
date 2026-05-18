package studio.fantasyit.ether_craft.integration.jei;

import com.mojang.serialization.Codec;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.ISlottedRecipeWidget;
import mezz.jei.api.helpers.ICodecHelper;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
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
    private static final int NODE_GAP = 6;
    private static final int PADDING = 4;
    static final int WIDTH = 140;
    static final int HEIGHT = 200;
    private static final int MIN_SPACING = 20;
    private static final int SCROLL_STEP = 20;
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

    private PanState panState;

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
    public void createRecipeExtras(IRecipeExtrasBuilder builder, EtherProcessFactoryRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotDrawablesView slotsView = builder.getRecipeSlots();
        List<IRecipeSlotDrawable> inputSlots = slotsView.getSlots(RecipeIngredientRole.INPUT);
        List<IRecipeSlotDrawable> chipSlots = slotsView.getSlots(RecipeIngredientRole.CRAFTING_STATION);
        List<IRecipeSlotDrawable> outputSlots = slotsView.getSlots(RecipeIngredientRole.OUTPUT);
        List<IRecipeSlotDrawable> allSlots = new ArrayList<>();
        allSlots.addAll(inputSlots);
        allSlots.addAll(chipSlots);
        allSlots.addAll(outputSlots);

        TreeLayout layout = TreeLayout.compute(recipe.json);

        List<PanState.SlotInfo> slotInfos = new ArrayList<>();
        int idx = 0;
        for (TreeLayout.Entry e : layout.inputs) {
            slotInfos.add(new PanState.SlotInfo(allSlots.get(idx), e.x, e.y));
            idx++;
        }
        for (TreeLayout.ChipEntry c : layout.chips) {
            slotInfos.add(new PanState.SlotInfo(allSlots.get(idx), c.x, c.y));
            idx++;
        }
        int outCount = recipe.json.output().item().size();
        for (int j = 0; j < outCount; j++) {
            int ox = layout.outputX + j * (SLOT_SIZE + 2);
            slotInfos.add(new PanState.SlotInfo(allSlots.get(idx + j), ox, layout.outputY));
        }

        int canvasW = layout.canvasWidth;
        int initialPan = canvasW > WIDTH ? canvasW - WIDTH : 0;
        PanState ps = new PanState(initialPan, canvasW, slotInfos, layout.edges);
        this.panState = ps;
        ps.applyPan();
        builder.addSlottedWidget(ps, allSlots);
        builder.addInputHandler(ps);
    }

    @Override
    public void draw(EtherProcessFactoryRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
    }

    @Override
    public Codec<EtherProcessFactoryRecipe> getCodec(ICodecHelper codecHelper, IRecipeManager recipeManager) {
        return EtherProcessFactoryRecipe.CODEC.codec();
    }

    @Override
    public @Nullable Identifier getIdentifier(EtherProcessFactoryRecipe recipe) {
        return null;
    }

    static class PanState implements ISlottedRecipeWidget, IJeiInputHandler {
        record SlotInfo(IRecipeSlotDrawable slot, int vx, int vy) {}

        int panX;
        final int canvasWidth;
        boolean canScrollLeft;
        boolean canScrollRight;
        final int leftX = WIDTH / 2 - 13;
        final int rightX = WIDTH / 2 + 5;

        final List<SlotInfo> slots;
        final List<TreeLayout.Edge> edges;

        final ArrowWidget leftArrow;
        final ArrowWidget rightArrow;

        PanState(int panX, int canvasWidth, List<SlotInfo> slots, List<TreeLayout.Edge> edges) {
            this.panX = panX;
            this.canvasWidth = canvasWidth;
            this.slots = slots;
            this.edges = edges;
            updateScrollFlags();
            this.leftArrow = new ArrowWidget(true, leftX, HEIGHT - 12);
            this.rightArrow = new ArrowWidget(false, rightX, HEIGHT - 12);
            leftArrow.visible = canScrollLeft;
            rightArrow.visible = canScrollRight;
        }

        private void updateScrollFlags() {
            canScrollLeft = panX > 0;
            canScrollRight = panX < canvasWidth - WIDTH;
        }

        void applyPan() {
            updateScrollFlags();
            leftArrow.visible = canScrollLeft;
            rightArrow.visible = canScrollRight;
            for (SlotInfo s : slots) {
                s.slot.setPosition(s.vx - panX, s.vy);
            }
        }

        @Override
        public ScreenRectangle getArea() {
            return new ScreenRectangle(0, 0, WIDTH, HEIGHT);
        }

        @Override
        public void drawWidget(GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            graphics.enableScissor(0, 0, WIDTH, HEIGHT);

            for (TreeLayout.Edge edge : edges) {
                drawLine(graphics, edge.fromX - panX, edge.fromY, edge.toX - panX, edge.toY);
            }

            for (SlotInfo s : slots) {
                s.slot.draw(graphics);
            }

            leftArrow.drawWidget(graphics, mouseX, mouseY);
            rightArrow.drawWidget(graphics, mouseX, mouseY);

            graphics.disableScissor();
        }

        @Override
        public Optional<RecipeSlotUnderMouse> getSlotUnderMouse(double mouseX, double mouseY) {
            for (SlotInfo s : slots) {
                if (s.slot.isMouseOver(mouseX, mouseY)) {
                    return Optional.of(new RecipeSlotUnderMouse(s.slot, new ScreenPosition(0, 0)));
                }
            }
            return Optional.empty();
        }

        @Override
        public ScreenPosition getPosition() {
            return new ScreenPosition(0, 0);
        }

        @Override
        public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
            if (canScrollLeft && mouseX >= leftX && mouseX < leftX + 10 && mouseY >= HEIGHT - 12 && mouseY < HEIGHT) {
                if (!input.isSimulate()) {
                    panX = Math.max(0, panX - SCROLL_STEP);
                    applyPan();
                }
                return true;
            }
            if (canScrollRight && mouseX >= rightX && mouseX < rightX + 10 && mouseY >= HEIGHT - 12 && mouseY < HEIGHT) {
                if (!input.isSimulate()) {
                    panX = Math.min(canvasWidth - WIDTH, panX + SCROLL_STEP);
                    applyPan();
                }
                return true;
            }
            return false;
        }
    }

    static class ArrowWidget {
        private final boolean left;
        private final int x, y;
        boolean visible = false;

        ArrowWidget(boolean left, int x, int y) {
            this.left = left;
            this.x = x;
            this.y = y;
        }

        void drawWidget(GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            if (!visible) return;
            Font font = Minecraft.getInstance().font;
            graphics.text(font, left ? Component.literal("◄") : Component.literal("►"), x, y, 0xFFFFFFFF);
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

    private static class TreeLayout {
        record Entry(String id, int x, int y, Ingredient ingredient) {}
        record ChipEntry(String parentId, int x, int y, Ingredient ingredient) {}
        record Edge(int fromX, int fromY, int toX, int toY) {}

        final List<Entry> inputs = new ArrayList<>();
        final List<ChipEntry> chips = new ArrayList<>();
        final List<Edge> edges = new ArrayList<>();
        int outputX, outputY, canvasWidth;

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

            int outCount = json.output().item().size();
            int outWidth = outCount * SLOT_SIZE + Math.max(0, outCount - 1) * 2;
            int usable = WIDTH - 2 * PADDING - outWidth;
            int spacing = maxLevel > 0 ? Math.max(MIN_SPACING, usable / maxLevel) : 0;
            layout.canvasWidth = PADDING + maxLevel * spacing + outWidth + PADDING;

            Map<Integer, List<String>> byLevel = new TreeMap<>();
            for (var e : levels.entrySet()) {
                byLevel.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
            }

            Map<String, Integer> nodeX = new HashMap<>();
            Map<String, Integer> nodeY = new HashMap<>();
            Map<String, Integer> nodeH = new HashMap<>();

            for (var entry : byLevel.entrySet()) {
                int level = entry.getKey();
                int colX = PADDING + (maxLevel - level) * spacing;
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

                int curY = PADDING + (HEIGHT - 2 * PADDING - totalH) / 2;
                for (String id : ids) {
                    nodeX.put(id, colX);
                    nodeY.put(id, curY);
                    curY += nodeH.get(id) + NODE_GAP;
                }
            }

            layout.outputX = layout.canvasWidth - PADDING - outWidth;
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
