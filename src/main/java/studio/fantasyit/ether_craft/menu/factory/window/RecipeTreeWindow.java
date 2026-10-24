package studio.fantasyit.ether_craft.menu.factory.window;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.display.DisplayContentsFactory;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeJson;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayoutComposer;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryAsset.SLOT;

/**
 * 由 JEI/EMI「+」配方转移创建的配方窗口：把一份 {@link EtherProcessFactoryRecipe} 的加工树画成树形图。
 *
 * <p>纯展示：数据在建窗时一次性从配方解析成物品（输入原料、工序芯片、输出），没有运行期状态，
 * 因此不显示进度条与以太边框。窗口几何、视口与输入处理由 {@link AbstractTreeWindow} 提供，
 * 与工厂实时预览窗口（{@code FactoryTreeOverlay}）共用同一套基础设施。
 */
public class RecipeTreeWindow extends AbstractTreeWindow {
    /** 树根的虚拟节点 id（配方输出）。 */
    private static final String OUT_ID = "out";
    /** 工序芯片节点边框色：与"原始输入"节点区分开。 */
    private static final int CHIP_BORDER_COLOR = 0xFFB0BEC5;

    private final Identifier recipeId;
    private final @Nullable EtherProcessRecipeJson json;
    private final Map<String, NodeVisual> visuals = new LinkedHashMap<>();
    private final Map<String, List<Component>> tooltips = new LinkedHashMap<>();

    /**
     * @param recipeId 配方 id（仅用于窗口标题回退）
     * @param recipe   配方本体
     */
    public RecipeTreeWindow(Identifier recipeId, EtherProcessFactoryRecipe recipe, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.recipeId = recipeId;
        this.json = recipe == null ? null : recipe.json;
        buildVisuals();
        rebuildLayout(true, true);
    }

    /** 单个节点的绘制数据。 */
    private record NodeVisual(boolean isInput, boolean isOutput, List<ItemStack> items) {
    }

    // ------------------------------------------------------------------ 数据

    private void buildVisuals() {
        if (json == null) return;
        ContextMap ctx = context();
        for (EtherProcessRecipeJson.InputEntry entry : json.input()) {
            List<ItemStack> options = resolve(entry.item(), ctx);
            List<ItemStack> first = options.isEmpty() ? List.of(ItemStack.EMPTY) : List.of(options.get(0));
            visuals.put(entry.id(), new NodeVisual(true, false, first));
            tooltips.put(entry.id(), List.of(Component.translatable(
                    "menu.ether_craft.factory.recipe_window.input", joinNames(options))
                    .withStyle(ChatFormatting.GRAY)));
        }
        for (EtherProcessRecipeJson.ProcessEntry entry : json.process()) {
            List<ItemStack> chips = new ArrayList<>();
            for (SizedIngredient sized : entry.item()) {
                List<ItemStack> options = resolve(sized, ctx);
                chips.add(options.isEmpty() ? ItemStack.EMPTY : options.get(0));
            }
            if (chips.isEmpty()) chips.add(ItemStack.EMPTY);
            visuals.put(entry.id(), new NodeVisual(false, false, List.copyOf(chips)));
            tooltips.put(entry.id(), chipTooltip(chips));
        }
        List<ItemStack> outputs = new ArrayList<>();
        for (ItemStackTemplate template : json.output().item()) {
            outputs.add(template.create());
        }
        if (outputs.isEmpty()) outputs.add(ItemStack.EMPTY);
        visuals.put(OUT_ID, new NodeVisual(false, true, List.copyOf(outputs)));
        tooltips.put(OUT_ID, List.of(Component.translatable(
                "menu.ether_craft.factory.recipe_window.output", joinNames(outputs))
                .withStyle(ChatFormatting.GOLD)));
    }

    private static @Nullable ContextMap context() {
        var level = Minecraft.getInstance().level;
        return level == null ? null : SlotDisplayContext.fromLevel(level);
    }

    private static List<ItemStack> resolve(@Nullable SizedIngredient sized, @Nullable ContextMap ctx) {
        if (sized == null || ctx == null) return List.of();
        return sized.ingredient().display()
                .resolve(ctx, (DisplayContentsFactory.ForStacks<ItemStack>)
                        stack -> stack.copyWithCount(sized.count()))
                .toList();
    }

    private static String joinNames(List<ItemStack> stacks) {
        List<String> names = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            names.add(stack.getHoverName().getString());
        }
        return String.join(" / ", names);
    }

    private static List<Component> chipTooltip(List<ItemStack> chips) {
        List<Component> lines = new ArrayList<>(chips.size() + 1);
        lines.add(Component.translatable("menu.ether_craft.factory.recipe_window.chip")
                .withStyle(ChatFormatting.GOLD));
        for (ItemStack chip : chips) {
            if (chip.isEmpty()) continue;
            lines.add(chip.getHoverName());
        }
        return lines;
    }

    // ------------------------------------------------------------------ 布局

    @Override
    protected void rebuildLayout(boolean force, boolean refit) {
        if (json == null) {
            widget.clearBindings();
            widget.clearDecorations();
            return;
        }
        TreeDiagramSpec spec = buildSpec(contentW());
        TreeDiagramLayoutComposer.Result result = TreeDiagramLayoutComposer.compose(
                List.of(new TreeDiagramLayoutComposer.GroupSpec("", spec)), contentW(), GROUP_GAP, 0);

        widget.clearBindings();
        widget.clearDecorations();
        widget.setLayout(result.layout());
        for (Map.Entry<String, NodeVisual> entry : visuals.entrySet()) {
            widget.bindNode(entry.getKey(), this::renderNode, this::nodeTooltip, null, entry.getValue());
        }
        if (refit) fitToView(result.layout());
    }

    /** 把配方 JSON 展成单棵树：输入叶子 + 工序芯片节点 + 一个输出节点。 */
    private TreeDiagramSpec buildSpec(int viewWidth) {
        List<TreeDiagramSpec.NodeSpec> nodes = new ArrayList<>();
        for (EtherProcessRecipeJson.InputEntry entry : json.input()) {
            nodes.add(new TreeDiagramSpec.NodeSpec(entry.id(), 1, resolveNext(entry.next()), true));
        }
        for (EtherProcessRecipeJson.ProcessEntry entry : json.process()) {
            int slots = Math.max(1, entry.item().size());
            nodes.add(new TreeDiagramSpec.NodeSpec(entry.id(), slots, resolveNext(entry.next()), false));
        }
        NodeVisual out = visuals.get(OUT_ID);
        int outSlots = out == null ? 1 : Math.max(1, out.items().size());
        nodes.add(new TreeDiagramSpec.NodeSpec(OUT_ID, outSlots, null, false));
        return new TreeDiagramSpec(nodes, new TreeDiagramSpec.OutputSpec(0),
                new TreeDiagramSpec.LayoutConfig(NODE_SIZE, NODE_SIZE, CHIP_GAP, 0,
                        viewWidth, 0, PADDING, NODE_GAP, MIN_SPACING));
    }

    /**
     * 解析 {@code next}：指向工序节点才算有效；指向输入 / 不存在 / 为空时，
     * 与 {@link EtherProcessFactoryRecipe} 建树逻辑一致——直接挂到输出（配方根）。
     */
    private String resolveNext(@Nullable String next) {
        if (next == null || json == null) return OUT_ID;
        for (EtherProcessRecipeJson.ProcessEntry entry : json.process()) {
            if (entry.id().equals(next)) return next;
        }
        return OUT_ID;
    }

    // ------------------------------------------------------------------ 渲染

    @Override
    protected Component title() {
        return Component.translatable("menu.ether_craft.factory.recipe_window.title", recipeName());
    }

    private String recipeName() {
        NodeVisual out = visuals.get(OUT_ID);
        if (out != null) {
            for (ItemStack stack : out.items()) {
                if (!stack.isEmpty()) return stack.getHoverName().getString();
            }
        }
        return recipeId == null ? "?" : recipeId.toString();
    }

    @Override
    protected boolean isEmptyModel() {
        return json == null || visuals.isEmpty();
    }

    @Override
    protected Component emptyText() {
        return Component.translatable("menu.ether_craft.factory.recipe_window.empty");
    }

    private void renderNode(GuiGraphicsExtractor graphics, TreeDiagramLayout.PositionedNode node,
                            NodeVisual visual, double mouseWorldX, double mouseWorldY) {
        int x = node.x();
        int y = node.y();
        for (int i = 0; i < visual.items().size(); i++) {
            int slotY = y + i * (NODE_SIZE + CHIP_GAP);
            SLOT.blit(graphics, x, slotY);
            ItemStack stack = visual.items().get(i);
            if (!stack.isEmpty()) graphics.item(stack, x + 1, slotY + 1);
            // 工序芯片节点画一圈淡边框，和"原始输入"节点区分开
            if (!visual.isInput() && !visual.isOutput()) drawBorder(graphics, x, slotY, CHIP_BORDER_COLOR);
        }
    }

    private void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x, y, x + NODE_SIZE, y + 1, color);
        graphics.fill(x, y + NODE_SIZE - 1, x + NODE_SIZE, y + NODE_SIZE, color);
        graphics.fill(x, y, x + 1, y + NODE_SIZE, color);
        graphics.fill(x + NODE_SIZE - 1, y, x + NODE_SIZE, y + NODE_SIZE, color);
    }

    private List<Component> nodeTooltip(NodeVisual visual, TreeDiagramLayout.PositionedNode node) {
        for (Map.Entry<String, NodeVisual> entry : visuals.entrySet()) {
            if (entry.getValue() != visual) continue;
            return tooltips.get(entry.getKey());
        }
        return null;
    }
}
