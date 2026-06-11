package studio.fantasyit.ether_craft.recipe.factory.render.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeDiagramWidget {
    private final int viewX, viewY;
    private final TreeDiagramViewport viewport;
    private final ViewportInputHandler inputHandler;
    private TreeDiagramLayout layout;
    private final Map<String, Binding<?>> bindings = new HashMap<>();
    private String hoveredNodeId;

    private static final class Binding<T> {
        final NodeRenderer<? super T> renderer;
        final NodeTooltipProvider<? super T> tooltip;
        final NodeClickHandler<? super T> click;
        final T data;

        Binding(NodeRenderer<? super T> renderer,
                NodeTooltipProvider<? super T> tooltip,
                NodeClickHandler<? super T> click,
                T data) {
            this.renderer = renderer;
            this.tooltip = tooltip;
            this.click = click;
            this.data = data;
        }

        @SuppressWarnings("unchecked")
        List<Component> getTooltip(TreeDiagramLayout.PositionedNode node) {
            return ((NodeTooltipProvider<T>) tooltip).getTooltip(data, node);
        }

        @SuppressWarnings("unchecked")
        void handleClick(TreeDiagramLayout.PositionedNode node, int button) {
            if (click != null) {
                ((NodeClickHandler<T>) click).onClick(data, node, button);
            }
        }
    }

    public TreeDiagramWidget(int viewX, int viewY, int viewWidth, int viewHeight) {
        this.viewX = viewX;
        this.viewY = viewY;
        this.viewport = new TreeDiagramViewport(0, 0, viewWidth, viewHeight);
        this.inputHandler = new ViewportInputHandler(viewport);
    }

    public void setLayout(TreeDiagramLayout layout) {
        this.layout = layout;
        this.viewport.canvasWidth = layout.canvasWidth;
        this.viewport.canvasHeight = layout.canvasHeight;
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
                if (binding != null) {
                    binding.handleClick(positioned, button);
                }
                return true;
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
