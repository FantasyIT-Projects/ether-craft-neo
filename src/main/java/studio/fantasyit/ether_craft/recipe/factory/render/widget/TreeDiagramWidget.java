package studio.fantasyit.ether_craft.recipe.factory.render.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeDiagramWidget {
    private int viewX, viewY;
    private final TreeDiagramViewport viewport;
    private final ViewportInputHandler inputHandler;
    private TreeDiagramLayout layout;
    private final Map<String, Binding<?>> bindings = new HashMap<>();
    private final List<Decoration<?>> decorations = new ArrayList<>();
    private String hoveredNodeId;

    private record Binding<T>(NodeRenderer<? super T> renderer, NodeTooltipProvider<? super T> tooltip,
                              NodeClickHandler<? super T> click, T data) {

        @SuppressWarnings("unchecked")
            List<Component> getTooltip(TreeDiagramLayout.PositionedNode node) {
                return tooltip.getTooltip(data, node);
            }

            @SuppressWarnings("unchecked")
            void handleClick(TreeDiagramLayout.PositionedNode node, int button) {
                if (click != null) {
                    click.onClick(data, node, button);
                }
            }
        }

    /** 只渲染、不参与命中测试的装饰块（如分组表头）。 */
    private record Decoration<T>(int x, int y, int width, int height,
                                 NodeRenderer<? super T> renderer, T data) {
    }

    public TreeDiagramWidget(int viewX, int viewY, int viewWidth, int viewHeight) {
        this.viewX = viewX;
        this.viewY = viewY;
        this.viewport = new TreeDiagramViewport(0, 0, viewWidth, viewHeight);
        this.inputHandler = new ViewportInputHandler(viewport);
    }

    public void setLayout(TreeDiagramLayout layout) {
        this.layout = layout;
        this.viewport.setCanvasSize(layout.canvasWidth, layout.canvasHeight);
        viewport.centerPan();
    }

    /** 浮层窗口移动/缩放后更新控件位置与视口尺寸（保留当前缩放与平移状态）。 */
    public void setBounds(int x, int y, int width, int height) {
        this.viewX = x;
        this.viewY = y;
        this.viewport.setViewSize(width, height);
    }

    /** 添加一个只渲染、不参与命中测试的装饰块（用于分组表头等）。 */
    public <T> void addDecoration(int x, int y, int width, int height, NodeRenderer<T> renderer, T data) {
        decorations.add(new Decoration<>(x, y, width, height, renderer, data));
    }

    public void clearDecorations() {
        decorations.clear();
    }

    public void clearBindings() {
        bindings.clear();
    }

    public <T> void bindNode(String id, NodeRenderer<T> renderer,
                              NodeTooltipProvider<T> tooltip,
                              NodeClickHandler<T> click, T data) {
        bindings.put(id, new Binding<>(renderer, tooltip, click, data));
    }

    public TreeDiagramViewport getViewport() {
        return viewport;
    }

    public TreeDiagramLayout getLayout() {
        return layout;
    }

    public void render(GuiGraphicsExtractor g, Font font, double mouseX, double mouseY) {
        if (layout == null) return;

        g.enableScissor(viewX, viewY, viewX + viewport.viewWidth, viewY + viewport.viewHeight);

        g.pose().pushMatrix();
        g.pose().translate((float) (viewX + viewport.panX), (float) (viewY + viewport.panY));
        g.pose().scale((float) viewport.zoom, (float) viewport.zoom);

        EdgeBatchRenderer.render(g, layout.edges);

        double worldMX = viewport.toWorldX(mouseX - viewX);
        double worldMY = viewport.toWorldY(mouseY - viewY);

        for (Decoration<?> decoration : decorations) {
            renderDecoration(decoration, g, worldMX, worldMY);
        }

        hoveredNodeId = null;
        for (var positioned : layout.nodes) {
            Binding<?> binding = bindings.get(positioned.id());
            if (binding != null) {
                renderNode(binding, g, positioned, worldMX, worldMY);
                if (mouseInNode(positioned, worldMX, worldMY)) {
                    hoveredNodeId = positioned.id();
                }
            }
        }

        g.pose().popMatrix();
        g.disableScissor();
    }

    public void renderTooltips(GuiGraphicsExtractor g, Font font, double mouseX, double mouseY) {
        if (hoveredNodeId == null) return;
        Binding<?> binding = bindings.get(hoveredNodeId);
        if (binding == null) return;
        List<Component> tip = binding.getTooltip(findNode(hoveredNodeId));
        if (tip != null) {
            g.setComponentTooltipForNextFrame(font, tip, (int) mouseX, (int) mouseY);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void renderDecoration(Decoration<T> decoration, GuiGraphicsExtractor g,
                                      double worldMX, double worldMY) {
        NodeRenderer<T> renderer = (NodeRenderer<T>) decoration.renderer;
        TreeDiagramLayout.PositionedNode area = new TreeDiagramLayout.PositionedNode(
                "decoration", decoration.x, decoration.y, decoration.width, decoration.height,
                decoration.height / 2, decoration.x + decoration.width);
        renderer.render(g, area, decoration.data, worldMX, worldMY);
    }

    @SuppressWarnings("unchecked")
    private <T> void renderNode(Binding<T> binding, GuiGraphicsExtractor g,
                                 TreeDiagramLayout.PositionedNode node,
                                 double worldMX, double worldMY) {
        NodeRenderer<T> renderer = (NodeRenderer<T>) binding.renderer;
        renderer.render(g, node, binding.data, worldMX, worldMY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isInBounds(mouseX, mouseY)) return false;

        double worldMX = viewport.toWorldX(mouseX - viewX);
        double worldMY = viewport.toWorldY(mouseY - viewY);

        for (var positioned : layout.nodes) {
            if (mouseInNode(positioned, worldMX, worldMY)) {
                Binding<?> binding = bindings.get(positioned.id());
                if (binding != null && binding.click != null) {
                    binding.handleClick(positioned, button);
                    return true;
                }
                // 命中节点但没有点击行为：继续走视口逻辑，使"从节点上按下"也能拖拽平移
                break;
            }
        }

        return inputHandler.handleMouseClicked(mouseX - viewX, mouseY - viewY,
                button, false, false);
    }

    public boolean mouseClickedWithSimulate(double mouseX, double mouseY, int button,
                                             boolean isSimulate) {
        if (!isInBounds(mouseX, mouseY)) return false;

        double worldMX = viewport.toWorldX(mouseX - viewX);
        double worldMY = viewport.toWorldY(mouseY - viewY);
        boolean isOverNode = false;
        for (var positioned : layout.nodes) {
            if (mouseInNode(positioned, worldMX, worldMY)) {
                isOverNode = true;
                break;
            }
        }

        return inputHandler.handleMouseClicked(mouseX - viewX, mouseY - viewY,
                button, isSimulate, isOverNode);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                 double dragX, double dragY) {
        return inputHandler.handleMouseDragged(mouseX - viewX, mouseY - viewY,
                button, dragX, dragY);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        inputHandler.onMouseReleased();
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isInBounds(mouseX, mouseY)) return false;
        return inputHandler.handleMouseScrolled(mouseX - viewX, mouseY - viewY, scrollY);
    }

    public boolean keyPressed(int keyCode) {
        return inputHandler.handleKey(keyCode);
    }

    private boolean isInBounds(double mouseX, double mouseY) {
        return mouseX >= viewX && mouseX < viewX + viewport.viewWidth
                && mouseY >= viewY && mouseY < viewY + viewport.viewHeight;
    }

    private static boolean mouseInNode(TreeDiagramLayout.PositionedNode node,
                                        double worldX, double worldY) {
        return worldX >= node.x() && worldX < node.x() + node.width()
                && worldY >= node.y() && worldY < node.y() + node.height();
    }

    private TreeDiagramLayout.PositionedNode findNode(String id) {
        for (var n : layout.nodes) {
            if (n.id().equals(id)) return n;
        }
        return null;
    }
}
