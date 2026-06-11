# Tree Diagram Decouple Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract tree diagram layout + rendering from JEI into a reusable three-layer component in `recipe/factory/render/`, then rebuild JEI integration on top.

**Architecture:** Layer 1 (pure Java data + BFS layout), Layer 2 (GuiGraphicsExtractor-based widget with pan/zoom/interaction), Layer 3 (scene adapters: JEI via ISlottedRecipeWidget, Factory GUI via NodeRenderer callbacks). JEI slot concept is fully isolated in Layer 3.

**Tech Stack:** Java 25, NeoForge MC 26.1.2, GuiGraphicsExtractor, Matrix3x2fStack, JEI 29.5.0.28

---

## File Map

```
CREATE  recipe/factory/render/data/TreeDiagramSpec.java          — Layer 1 input
CREATE  recipe/factory/render/data/TreeDiagramLayout.java        — Layer 1 output
CREATE  recipe/factory/render/data/TreeLayoutCalculator.java     — Layer 1 algorithm
CREATE  recipe/factory/render/widget/NodeRenderer.java           — Layer 2 callback IF
CREATE  recipe/factory/render/widget/NodeTooltipProvider.java    — Layer 2 callback IF
CREATE  recipe/factory/render/widget/NodeClickHandler.java       — Layer 2 callback IF
CREATE  recipe/factory/render/widget/TreeDiagramViewport.java    — Layer 2 pan/zoom
CREATE  recipe/factory/render/widget/ViewportInputHandler.java   — Layer 2 input
CREATE  recipe/factory/render/widget/EdgeBatchRenderer.java      — Layer 2 edge drawing
CREATE  recipe/factory/render/widget/TreeDiagramWidget.java      — Layer 2 compositor
CREATE  integration/jei/JEITreeSlottedWidget.java                — Layer 3 JEI adapter
MODIFY  integration/jei/TreeLayout.java                          — delegate to Layer 1
MODIFY  integration/jei/EtherProcessCategory.java                — use JEITreeSlottedWidget
DELETE  integration/jei/ViewportTransform.java                   — replaced
DELETE  integration/jei/ViewportSlotProxy.java                   — replaced
```

---

### Task 1: Create `TreeDiagramSpec.java`

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/data/TreeDiagramSpec.java`

- [ ] **Step 1: Write TreeDiagramSpec.java**

```java
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
                18, 22, 1, 9, 140, 90, 4, 15, 26
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
```

- [ ] **Step 2: Build to verify compilation**

Run: `idea_build_project` with file `src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/data/TreeDiagramSpec.java`
Expected: BUILD SUCCESS

---

### Task 2: Create `TreeDiagramLayout.java`

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/data/TreeDiagramLayout.java`

- [ ] **Step 1: Write TreeDiagramLayout.java**

```java
package studio.fantasyit.ether_craft.recipe.factory.render.data;

import java.util.ArrayList;
import java.util.List;

public final class TreeDiagramLayout {
    public final List<PositionedNode> nodes = new ArrayList<>();
    public final List<Edge> edges = new ArrayList<>();
    public PositionedOutput output;
    public int canvasWidth;
    public int canvasHeight;

    public record PositionedNode(
            String id,
            int x, int y,
            int width, int height,
            int midY,
            int exitX
    ) {
    }

    public record PositionedOutput(
            int x, int y,
            int width, int height,
            int midY
    ) {
    }

    public record Edge(int fromX, int fromY, int toX, int toY) {
    }
}
```

- [ ] **Step 2: Build to verify compilation**

Run: `idea_build_project` with file `src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/data/TreeDiagramLayout.java`
Expected: BUILD SUCCESS

---

### Task 3: Create `TreeLayoutCalculator.java`

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/data/TreeLayoutCalculator.java`

- [ ] **Step 1: Write TreeLayoutCalculator.java**

```java
package studio.fantasyit.ether_craft.recipe.factory.render.data;

import java.util.*;

public final class TreeLayoutCalculator {

    public static TreeDiagramLayout compute(TreeDiagramSpec spec) {
        TreeDiagramLayout layout = new TreeDiagramLayout();
        TreeDiagramSpec.LayoutConfig cfg = spec.config();

        Set<String> allIds = new HashSet<>();
        Map<String, Boolean> isInput = new HashMap<>();
        Map<String, String> nextMap = new HashMap<>();
        for (var node : spec.nodes()) {
            allIds.add(node.id());
            isInput.put(node.id(), node.isInput());
            nextMap.put(node.id(), node.nextId());
        }

        Map<String, Integer> nodeHeights = new HashMap<>();
        for (var node : spec.nodes()) {
            if (node.isInput()) {
                nodeHeights.put(node.id(), cfg.slotSize());
            } else {
                int cnt = node.subSlotCount();
                nodeHeights.put(node.id(),
                        cnt * cfg.slotSize() + (cnt - 1) * cfg.chipGap() - (cnt - 1) * cfg.chipOverlap());
            }
        }

        Map<String, Integer> levels = computeLevels(spec.nodes(), allIds);

        int maxLevel = levels.values().stream().max(Integer::compareTo).orElse(0);

        int outCount = spec.output().slotCount();
        int outHeight = outCount * cfg.slotSize() + Math.max(0, outCount - 1) * cfg.chipGap();
        int outWidth = cfg.slotSize();
        int usable = cfg.viewWidth() - 2 * cfg.padding() - outWidth;
        int spacing = maxLevel > 0 ? Math.max(cfg.minSpacing(), usable / maxLevel) : 0;
        layout.canvasWidth = cfg.padding() + maxLevel * spacing + outWidth + cfg.padding();

        Map<Integer, List<String>> byLevel = new TreeMap<>();
        for (var e : levels.entrySet()) {
            byLevel.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }

        Map<String, Integer> nodeX = new HashMap<>();
        Map<String, Integer> nodeY = new HashMap<>();

        for (var entry : byLevel.entrySet()) {
            int level = entry.getKey();
            int colX = cfg.padding() + (maxLevel - level) * spacing;
            List<String> ids = new ArrayList<>(entry.getValue());

            if (level == 1) {
                ids.sort(String::compareTo);
            } else {
                ids.sort((a, b) -> {
                    String na = nextMap.get(a);
                    String nb = nextMap.get(b);
                    int ya = na != null && nodeY.containsKey(na) ? nodeY.get(na) : 0;
                    int yb = nb != null && nodeY.containsKey(nb) ? nodeY.get(nb) : 0;
                    if (ya != yb) return Integer.compare(ya, yb);
                    return a.compareTo(b);
                });
            }

            int totalH = 0;
            for (String id : ids) {
                totalH += nodeHeights.get(id) + cfg.nodeGap();
            }
            if (totalH > 0) totalH -= cfg.nodeGap();

            int curY = cfg.padding() + (cfg.viewHeight() - 2 * cfg.padding() - totalH) / 2;
            for (String id : ids) {
                nodeX.put(id, colX);
                nodeY.put(id, curY);
                curY += nodeHeights.get(id) + cfg.nodeGap();
            }
        }

        layout.output = new TreeDiagramLayout.PositionedOutput(
                layout.canvasWidth - cfg.padding() - outWidth,
                cfg.padding() + (cfg.viewHeight() - 2 * cfg.padding() - outHeight) / 2,
                outWidth,
                outHeight,
                outHeight / 2
        );

        layout.canvasHeight = cfg.viewHeight();

        // Construct positioned nodes
        for (var nodeSpec : spec.nodes()) {
            int h = nodeHeights.get(nodeSpec.id());
            int x = nodeX.get(nodeSpec.id());
            int y = nodeY.get(nodeSpec.id());
            int w = cfg.slotSize();
            if (!nodeSpec.isInput()) {
                // Process nodes are one slot wide but multiple slots tall
            }
            layout.nodes.add(new TreeDiagramLayout.PositionedNode(
                    nodeSpec.id(), x, y, w, h,
                    isInput.get(nodeSpec.id()) ? cfg.slotSize() / 2 : h / 2,
                    x + cfg.slotSize()
            ));
        }

        // Compute edges
        for (var nodeSpec : spec.nodes()) {
            int fx = nodeX.get(nodeSpec.id()) + cfg.slotSize();
            int fy = nodeY.get(nodeSpec.id())
                    + (isInput.get(nodeSpec.id()) ? cfg.slotSize() / 2 : nodeHeights.get(nodeSpec.id()) / 2);
            String next = nodeSpec.nextId();
            int tx, ty;
            if (next != null && allIds.contains(next)) {
                tx = nodeX.get(next);
                ty = nodeY.get(next) + (isInput.get(next) ? cfg.slotSize() / 2 : nodeHeights.get(next) / 2);
            } else {
                tx = layout.output.x();
                ty = layout.output.y() + outHeight / 2;
            }
            layout.edges.add(new TreeDiagramLayout.Edge(fx, fy, tx, ty));
        }

        return layout;
    }

    private static Map<String, Integer> computeLevels(
            List<TreeDiagramSpec.NodeSpec> nodes, Set<String> allIds) {
        Map<String, Integer> levels = new HashMap<>();
        Map<String, List<String>> predecessors = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>();

        for (var node : nodes) {
            String next = node.nextId();
            if (next != null && allIds.contains(next)) {
                predecessors.computeIfAbsent(next, k -> new ArrayList<>()).add(node.id());
            } else {
                levels.put(node.id(), 1);
                queue.add(node.id());
            }
        }

        while (!queue.isEmpty()) {
            String id = queue.poll();
            int level = levels.get(id);
            for (String pred : predecessors.getOrDefault(id, List.of())) {
                if (!levels.containsKey(pred)) {
                    levels.put(pred, level + 1);
                    queue.add(pred);
                }
            }
        }

        for (String id : allIds) {
            levels.putIfAbsent(id, 1);
        }

        return levels;
    }
}
```

- [ ] **Step 2: Build to verify compilation**

Run: `idea_build_project` with file `src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/data/TreeLayoutCalculator.java`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit Layer 1**

```bash
git add src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/data/
git commit -m "feat: add Layer 1 — TreeDiagramSpec, TreeDiagramLayout, TreeLayoutCalculator"
```

---

### Task 4: Create callback interfaces (NodeRenderer, NodeTooltipProvider, NodeClickHandler)

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/widget/NodeRenderer.java`
- Create: `src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/widget/NodeTooltipProvider.java`
- Create: `src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/widget/NodeClickHandler.java`

- [ ] **Step 1: Write NodeRenderer.java**

```java
package studio.fantasyit.ether_craft.recipe.factory.render.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;

@FunctionalInterface
public interface NodeRenderer<T> {
    void render(GuiGraphicsExtractor g, TreeDiagramLayout.PositionedNode node, T data,
                double mouseWorldX, double mouseWorldY);
}
```

- [ ] **Step 2: Write NodeTooltipProvider.java**

```java
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
```

- [ ] **Step 3: Write NodeClickHandler.java**

```java
package studio.fantasyit.ether_craft.recipe.factory.render.widget;

import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;

@FunctionalInterface
public interface NodeClickHandler<T> {
    void onClick(T data, TreeDiagramLayout.PositionedNode node, int button);
}
```

- [ ] **Step 4: Build**

Run: `idea_build_project` with all three files
Expected: BUILD SUCCESS

---

### Task 5: Create `TreeDiagramViewport.java`

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/widget/TreeDiagramViewport.java`

- [ ] **Step 1: Write TreeDiagramViewport.java**

```java
package studio.fantasyit.ether_craft.recipe.factory.render.widget;

import net.minecraft.util.Mth;

public class TreeDiagramViewport {
    double panX, panY;
    double zoom = 1.0;
    final double minZoom;
    final int canvasWidth, canvasHeight;
    final int viewWidth, viewHeight;

    public TreeDiagramViewport(int canvasWidth, int canvasHeight, int viewWidth, int viewHeight) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.minZoom = Math.min(1.0, Math.min(
                (double) viewWidth / canvasWidth,
                (double) viewHeight / canvasHeight));
    }

    public double toWorldX(double screenX) {
        return (screenX - panX) / zoom;
    }

    public double toWorldY(double screenY) {
        return (screenY - panY) / zoom;
    }

    public double toScreenX(double worldX) {
        return worldX * zoom + panX;
    }

    public double toScreenY(double worldY) {
        return worldY * zoom + panY;
    }

    public void clampPan() {
        double lowerX = Math.min(0, viewWidth - canvasWidth * zoom);
        double upperX = Math.max(0, viewWidth - canvasWidth * zoom);
        double lowerY = Math.min(0, viewHeight - canvasHeight * zoom);
        double upperY = Math.max(0, viewHeight - canvasHeight * zoom);
        panX = Mth.clamp(panX, lowerX, upperX);
        panY = Mth.clamp(panY, lowerY, upperY);
    }

    public void zoomAt(double screenX, double screenY, double factor) {
        double oldZoom = zoom;
        double newZoom = Mth.clamp(zoom * factor, minZoom, 1.0);
        if (newZoom == oldZoom) return;
        double ratio = newZoom / oldZoom;
        panX = screenX - (screenX - panX) * ratio;
        panY = screenY - (screenY - panY) * ratio;
        zoom = newZoom;
        clampPan();
    }

    public double getPanX() { return panX; }
    public double getPanY() { return panY; }
    public double getZoom() { return zoom; }
}
```

- [ ] **Step 2: Build**

Run: `idea_build_project` with file `src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/widget/TreeDiagramViewport.java`
Expected: BUILD SUCCESS

---

### Task 6: Create `ViewportInputHandler.java`

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/widget/ViewportInputHandler.java`

- [ ] **Step 1: Write ViewportInputHandler.java**

```java
package studio.fantasyit.ether_craft.recipe.factory.render.widget;

import org.lwjgl.glfw.GLFW;

public class ViewportInputHandler {
    private final TreeDiagramViewport viewport;
    private boolean dragging;

    public ViewportInputHandler(TreeDiagramViewport viewport) {
        this.viewport = viewport;
    }

    public boolean handleKey(int keyCode) {
        double step = 20.0;
        boolean handled = switch (keyCode) {
            case GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP -> {
                viewport.panY += step;
                yield true;
            }
            case GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> {
                viewport.panY -= step;
                yield true;
            }
            case GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT -> {
                viewport.panX += step;
                yield true;
            }
            case GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT -> {
                viewport.panX -= step;
                yield true;
            }
            case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> {
                viewport.zoomAt(viewport.viewWidth / 2.0, viewport.viewHeight / 2.0, 1.1);
                yield true;
            }
            case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> {
                viewport.zoomAt(viewport.viewWidth / 2.0, viewport.viewHeight / 2.0, 1.0 / 1.1);
                yield true;
            }
            default -> false;
        };
        if (handled) {
            viewport.clampPan();
        }
        return handled;
    }

    public boolean handleMouseClicked(double mouseX, double mouseY, int button,
                                       boolean isSimulate, boolean isOverNode) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (isSimulate) {
                dragging = !isOverNode;
                return dragging;
            }
            dragging = false;
            return false;
        }
        return false;
    }

    public boolean handleMouseDragged(double mouseX, double mouseY, int mouseKey,
                                       double dragX, double dragY) {
        if (dragging && mouseKey == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            viewport.panX += dragX;
            viewport.panY += dragY;
            viewport.clampPan();
            return true;
        }
        return false;
    }

    public boolean handleMouseScrolled(double mouseX, double mouseY, double scrollDeltaY) {
        if (scrollDeltaY != 0) {
            double factor = scrollDeltaY > 0 ? 1.1 : 1.0 / 1.1;
            viewport.zoomAt(mouseX, mouseY, factor);
            return true;
        }
        return false;
    }

    public void onMouseReleased() {
        dragging = false;
    }
}
```

- [ ] **Step 2: Build**

Run: `idea_build_project` with file `src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/widget/ViewportInputHandler.java`
Expected: BUILD SUCCESS

---

### Task 7: Create `EdgeBatchRenderer.java`

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/widget/EdgeBatchRenderer.java`

- [ ] **Step 1: Write EdgeBatchRenderer.java**

```java
package studio.fantasyit.ether_craft.recipe.factory.render.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;

import java.util.List;

public final class EdgeBatchRenderer {
    private static final int LINE_COLOR = 0xFFAAAAAA;

    public static void render(GuiGraphicsExtractor g, List<TreeDiagramLayout.Edge> edges) {
        for (var edge : edges) {
            drawLine(g, edge.fromX(), edge.fromY(), edge.toX(), edge.toY());
        }
    }

    public static void render(GuiGraphicsExtractor g, List<TreeDiagramLayout.Edge> edges, int lineColor) {
        for (var edge : edges) {
            drawLine(g, edge.fromX(), edge.fromY(), edge.toX(), edge.toY(), lineColor);
        }
    }

    private static void drawLine(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2) {
        drawLine(g, x1, y1, x2, y2, LINE_COLOR);
    }

    private static void drawLine(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        int midX = (x1 + x2) / 2;
        g.horizontalLine(x1, midX, y1, color);
        g.verticalLine(midX, y1, y2, color);
        g.horizontalLine(midX, x2, y2, color);
    }
}
```

- [ ] **Step 2: Build**

Run: `idea_build_project` with file `src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/widget/EdgeBatchRenderer.java`
Expected: BUILD SUCCESS

---

### Task 8: Create `TreeDiagramWidget.java`

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/widget/TreeDiagramWidget.java`

- [ ] **Step 1: Write TreeDiagramWidget.java**

```java
package studio.fantasyit.ether_craft.recipe.factory.render.widget;

import net.minecraft.client.Minecraft;
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

    // --- Render ---

    public void render(GuiGraphicsExtractor g, Font font, double mouseX, double mouseY) {
        if (layout == null) return;

        g.enableScissor(viewX, viewY, viewX + viewport.viewWidth, viewY + viewport.viewHeight);

        g.pose().pushMatrix();
        g.pose().translate((float)(viewX + viewport.panX), (float)(viewY + viewport.panY));
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
        List<Component> tip = binding.tooltip.getTooltip(
                binding.data, findNode(hoveredNodeId));
        if (tip != null) {
            g.setTooltipForNextFrame(font, tip, (int) mouseX, (int) mouseY);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void renderNode(Binding<T> binding, GuiGraphicsExtractor g,
                                 TreeDiagramLayout.PositionedNode node,
                                 double worldMX, double worldMY) {
        NodeRenderer<T> renderer = (NodeRenderer<T>) binding.renderer;
        renderer.render(g, node, binding.data, worldMX, worldMY);
    }

    // --- Input ---

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isInBounds(mouseX, mouseY)) return false;

        double worldMX = viewport.toWorldX(mouseX - viewX);
        double worldMY = viewport.toWorldY(mouseY - viewY);

        // Check node hit first
        for (var positioned : layout.nodes) {
            if (mouseInNode(positioned, worldMX, worldMY)) {
                Binding<?> binding = bindings.get(positioned.id());
                if (binding != null && binding.click != null) {
                    binding.click.onClick(binding.data, positioned, button);
                }
                return true;
            }
        }

        return inputHandler.handleMouseClicked(mouseX - viewX, mouseY - viewY,
                button, false, false);
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

    // --- Helpers ---

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
```

- [ ] **Step 2: Build to verify Layer 2**

Run: `idea_build_project` with all files under `src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/widget/`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit Layer 2**

```bash
git add src/main/java/studio/fantasyit/ether_craft/recipe/factory/render/widget/
git commit -m "feat: add Layer 2 — TreeDiagramWidget with viewport, input, edge rendering"
```

---

### Task 9: Modify `TreeLayout.java` to delegate to Layer 1

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/integration/jei/TreeLayout.java`

- [ ] **Step 1: Rewrite TreeLayout.compute() to delegate**

Change the `compute()` method from direct algorithm to delegation. The file content becomes:

```java
package studio.fantasyit.ether_craft.integration.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.DisplayContentsFactory;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeJson;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramSpec;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeLayoutCalculator;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;

import net.minecraft.util.context.ContextMap;

import java.util.*;

public class TreeLayout {
    static final int SLOT_SIZE = 18;
    static final int SLOT_SIZE_OUTPUT = 22;
    static final int CHIP_GAP = 1;
    static final int NODE_GAP = 15;
    static final int PADDING = 4;
    static final int WIDTH = 140;
    static final int HEIGHT = 90;
    static final int MIN_SPACING = 26;

    record Entry(String id, int x, int y, SizedIngredient ingredient) {}
    record ChipEntry(String parentId, int x, int y, SizedIngredient ingredient) {}
    record Edge(int fromX, int fromY, int toX, int toY) {}

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

        Map<String, List<SizedIngredient>> processIngredients = new HashMap<>();
        for (var proc : json.process()) {
            processIngredients.put(proc.id(), proc.item());
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
```

- [ ] **Step 2: Build**

Run: `idea_build_project` with file `src/main/java/studio/fantasyit/ether_craft/integration/jei/TreeLayout.java`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/integration/jei/TreeLayout.java
git commit -m "refactor: delegate TreeLayout.compute() to TreeLayoutCalculator"
```

---

### Task 10: Create `JEITreeSlottedWidget.java`

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/integration/jei/JEITreeSlottedWidget.java`

- [ ] **Step 1: Write JEITreeSlottedWidget.java**

```java
package studio.fantasyit.ether_craft.integration.jei;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import mezz.jei.api.gui.widgets.ISlottedRecipeWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.lwjgl.glfw.GLFW;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;
import studio.fantasyit.ether_craft.recipe.factory.render.widget.EdgeBatchRenderer;
import studio.fantasyit.ether_craft.recipe.factory.render.widget.TreeDiagramViewport;
import studio.fantasyit.ether_craft.recipe.factory.render.widget.ViewportInputHandler;

import java.util.*;

public class JEITreeSlottedWidget implements ISlottedRecipeWidget, IJeiInputHandler {
    private static final int VIEW_W = 140;
    private static final int VIEW_H = 90;

    private final TreeDiagramViewport viewport;
    private final ViewportInputHandler inputHandler;
    private final TreeDiagramLayout layout;
    private final List<IRecipeSlotDrawable> allSlots;
    private final Map<String, TreeDiagramLayout.PositionedNode> nodeById = new HashMap<>();
    private final Map<String, IRecipeSlotDrawable> slotById = new HashMap<>();

    public JEITreeSlottedWidget(TreeDiagramLayout layout,
                                 List<IRecipeSlotDrawable> allSlots) {
        this.layout = layout;
        this.allSlots = new ArrayList<>(allSlots);
        this.viewport = new TreeDiagramViewport(layout.canvasWidth, layout.canvasHeight, VIEW_W, VIEW_H);
        this.inputHandler = new ViewportInputHandler(viewport);
        for (var node : layout.nodes) {
            nodeById.put(node.id(), node);
        }
    }

    public void registerSlot(String nodeId, IRecipeSlotDrawable slot) {
        slotById.put(nodeId, slot);
    }

    public List<IRecipeSlotDrawable> getAllSlots() {
        return allSlots;
    }

    // --- ISlottedRecipeWidget ---

    @Override
    public ScreenRectangle getArea() {
        return new ScreenRectangle(0, 0, VIEW_W, VIEW_H);
    }

    @Override
    public ScreenPosition getPosition() {
        return new ScreenPosition(0, 0);
    }

    @Override
    public void drawWidget(GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        graphics.enableScissor(0, 0, VIEW_W, VIEW_H);
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) viewport.panX, (float) viewport.panY);
        graphics.pose().scale((float) viewport.zoom, (float) viewport.zoom);

        EdgeBatchRenderer.render(graphics, layout.edges);

        for (var slot : allSlots) {
            slot.draw(graphics);
        }

        graphics.pose().popMatrix();
        graphics.disableScissor();
    }

    @Override
    public Optional<RecipeSlotUnderMouse> getSlotUnderMouse(double mouseX, double mouseY) {
        double worldMX = viewport.toWorldX(mouseX);
        double worldMY = viewport.toWorldY(mouseY);
        for (var entry : slotById.entrySet()) {
            IRecipeSlotDrawable slot = entry.getValue();
            if (slot.isMouseOver(worldMX, worldMY)) {
                return Optional.of(new RecipeSlotUnderMouse(slot, getPosition()));
            }
        }
        return Optional.empty();
    }

    // --- IJeiInputHandler ---

    @Override
    public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
        int key = input.getKey().getValue();

        if (input.getKey().getType() == InputConstants.Type.KEYSYM) {
            boolean handled = inputHandler.handleKey(key);
            if (handled && !input.isSimulate()) {
                viewport.clampPan();
            }
            return handled;
        }

        if (input.getKey().getType() == InputConstants.Type.MOUSE
                && key == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            boolean isOverSlot = getSlotUnderMouse(mouseX, mouseY).isPresent();
            boolean handled = inputHandler.handleMouseClicked(mouseX, mouseY,
                    key, input.isSimulate(), isOverSlot);
            if (input.isSimulate()) {
                return handled;
            }
            return false;
        }

        return false;
    }

    @Override
    public boolean handleMouseScrolled(double mouseX, double mouseY,
                                        double scrollDeltaX, double scrollDeltaY) {
        return inputHandler.handleMouseScrolled(mouseX, mouseY, scrollDeltaY);
    }

    @Override
    public boolean handleMouseDragged(double mouseX, double mouseY,
                                       InputConstants.Key mouseKey,
                                       double dragX, double dragY) {
        return inputHandler.handleMouseDragged(mouseX, mouseY,
                mouseKey.getValue(), dragX, dragY);
    }
}
```

- [ ] **Step 2: Build**

Run: `idea_build_project` with file `src/main/java/studio/fantasyit/ether_craft/integration/jei/JEITreeSlottedWidget.java`
Expected: BUILD SUCCESS

---

### Task 11: Modify `EtherProcessCategory.java` to use `JEITreeSlottedWidget`

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/integration/jei/EtherProcessCategory.java`

- [ ] **Step 1: Rewrite createRecipeExtras()**

In `EtherProcessCategory.java`, replace the `createRecipeExtras` method (lines 100-137) and `setRecipe` method (lines 73-98) to use the new `JEITreeSlottedWidget`.

The full updated file becomes:

```java
package studio.fantasyit.ether_craft.integration.jei;

import com.mojang.serialization.Codec;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.ICodecHelper;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramSpec;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeLayoutCalculator;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;

import java.util.ArrayList;
import java.util.List;

public class EtherProcessCategory implements IRecipeCategory<EtherProcessFactoryRecipe> {
    private final IDrawable icon;
    private final IRecipeType<EtherProcessFactoryRecipe> recipeType;
    private final Component title;

    public EtherProcessCategory(IGuiHelper guiHelper,
                                IRecipeType<EtherProcessFactoryRecipe> recipeType,
                                Component title,
                                ItemStack iconStack) {
        this.recipeType = recipeType;
        this.title = title;
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                iconStack
        );
    }

    @Override
    public IRecipeType<EtherProcessFactoryRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return TreeLayout.WIDTH;
    }

    @Override
    public int getHeight() {
        return TreeLayout.HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EtherProcessFactoryRecipe recipe, IFocusGroup focuses) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        ContextMap ctx = SlotDisplayContext.fromLevel(level);

        TreeDiagramSpec spec = TreeDiagramSpec.fromJson(recipe.json);
        TreeDiagramLayout layout = TreeLayoutCalculator.compute(spec);
        TreeLayout legacy = TreeLayout.compute(recipe.json);

        for (TreeLayout.Entry e : legacy.inputs) {
            builder.addInputSlot(e.x(), e.y())
                    .addItemStacks(TreeLayout.resolveSizedIngredient(e.ingredient(), ctx))
                    .setStandardSlotBackground();
        }
        for (TreeLayout.ChipEntry e : legacy.chips) {
            builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, e.x(), e.y())
                    .addItemStacks(TreeLayout.resolveSizedIngredient(e.ingredient(), ctx))
                    .setStandardSlotBackground();
        }
        int outX = layout.output.x();
        for (var item : recipe.json.output().item()) {
            builder.addOutputSlot(outX, layout.output.y())
                    .add(item)
                    .setOutputSlotBackground();
            outX += TreeLayout.SLOT_SIZE + 2;
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, EtherProcessFactoryRecipe recipe, IFocusGroup focuses) {
        var slotsView = builder.getRecipeSlots();
        List<IRecipeSlotDrawable> inputSlots = slotsView.getSlots(RecipeIngredientRole.INPUT);
        List<IRecipeSlotDrawable> chipSlots = slotsView.getSlots(RecipeIngredientRole.CRAFTING_STATION);
        List<IRecipeSlotDrawable> outputSlots = slotsView.getSlots(RecipeIngredientRole.OUTPUT);
        List<IRecipeSlotDrawable> allSlots = new ArrayList<>();
        allSlots.addAll(inputSlots);
        allSlots.addAll(chipSlots);
        allSlots.addAll(outputSlots);

        TreeDiagramSpec spec = TreeDiagramSpec.fromJson(recipe.json);
        TreeDiagramLayout layout = TreeLayoutCalculator.compute(spec);

        JEITreeSlottedWidget widget = new JEITreeSlottedWidget(layout, allSlots);

        int idx = 0;
        for (var node : layout.nodes) {
            if (idx < allSlots.size()) {
                IRecipeSlotDrawable slot = allSlots.get(idx);
                slot.setPosition(node.x(), node.y());
                widget.registerSlot(node.id(), slot);
                idx++;
            }
        }
        // Output slots are already at the end of allSlots; they were positioned in setRecipe()

        builder.addSlottedWidget(widget, allSlots);
        builder.addInputHandler(widget);
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
}
```

- [ ] **Step 2: Build**

Run: `idea_build_project` with file `src/main/java/studio/fantasyit/ether_craft/integration/jei/EtherProcessCategory.java`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/integration/jei/JEITreeSlottedWidget.java src/main/java/studio/fantasyit/ether_craft/integration/jei/EtherProcessCategory.java
git commit -m "feat: add JEITreeSlottedWidget, switch EtherProcessCategory to use Layer 2 viewport"
```

---

### Task 12: Remove `ViewportTransform.java` and `ViewportSlotProxy.java`

**Files:**
- Delete: `src/main/java/studio/fantasyit/ether_craft/integration/jei/ViewportTransform.java`
- Delete: `src/main/java/studio/fantasyit/ether_craft/integration/jei/ViewportSlotProxy.java`

- [ ] **Step 1: Verify no remaining references**

Search for any remaining imports of `ViewportTransform` or `ViewportSlotProxy` across the project:
```bash
# manual: check any source still references these classes
```

Expected: No references (both were only used by `EtherProcessCategory`, which was updated in Task 11).

- [ ] **Step 2: Delete files**

Delete `src/main/java/studio/fantasyit/ether_craft/integration/jei/ViewportTransform.java`
Delete `src/main/java/studio/fantasyit/ether_craft/integration/jei/ViewportSlotProxy.java`

- [ ] **Step 3: Build to verify**

Run: `idea_build_project`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git rm src/main/java/studio/fantasyit/ether_craft/integration/jei/ViewportTransform.java
git rm src/main/java/studio/fantasyit/ether_craft/integration/jei/ViewportSlotProxy.java
git commit -m "refactor: remove ViewportTransform and ViewportSlotProxy (replaced by JEITreeSlottedWidget)"
```

---

### Task 13: Final verification — full project build

- [ ] **Step 1: Full rebuild**

Run: `idea_build_project` with `rebuild=true`
Expected: BUILD SUCCESS, no errors

- [ ] **Step 2: Verify JEI integration works at runtime**

Run the client via `runClient` run configuration. Open JEI, view an Ether Process recipe. The tree diagram should render identically to before with pan/zoom interaction intact.

---

### Task 14: Clean up old method in TreeLayout (optional)

- [ ] **Step 1: Remove dead code**

In `TreeLayout.java`, remove the internal methods that are no longer called: `computeLevels()`, `nodeMidY()`. These were extracted into `TreeLayoutCalculator` and should only be there.

- [ ] **Step 2: Build and commit**

Run: `idea_build_project`
```bash
git add src/main/java/studio/fantasyit/ether_craft/integration/jei/TreeLayout.java
git commit -m "chore: remove dead code from TreeLayout (now in TreeLayoutCalculator)"
```
