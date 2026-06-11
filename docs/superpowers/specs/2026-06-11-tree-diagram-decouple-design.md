# Tree Diagram Decouple Design

**Date**: 2026-06-11
**Status**: Draft

## Goal

Decouple the tree diagram rendering from JEI (`EtherProcessCategory`) into a reusable three-layer architecture, enabling the same tree to render in JEI, the Factory block GUI, and standalone screens — while maximizing data structure reuse with JEI.

## Architecture Overview

Three layers, progressing from pure data → Minecraft GUI → scene-specific adapters:

```
Layer 1 (data)          Layer 2 (widget)            Layer 3 (adapters)
─────────────────       ──────────────────          ──────────────────

                        ┌─ TreeDiagramViewport ─┐
TreeLayoutCalculator ───┤  panX/panY/zoom/clamp  │
  BFS layout            │  screen↔world transform│
                         │                        │
                        ├─ ViewportInputHandler ──┤
                        │  WASD/drag/scroll       │
                        │                        │
                        └─ EdgeBatchRenderer ─────┘
                          折线 fill 绘制
                                    │
                  ┌─────────────────┼─────────────────┐
                  ▼                 ▼                 ▼
           JEITreeSlottedWidget  FactoryScreen     StandaloneScreen
           (IRecipeSlotDrawable)  (ItemStack+       (ItemStack)
                                   progress bar)
```

**Key principle**: JEI slot concept is **completely isolated in Layer 3** (`JEITreeSlottedWidget`). Layer 1 and Layer 2 have zero JEI dependencies.

## Package Structure

```
studio.fantasyit.ether_craft.recipe.factory.render/   (NEW)
├── data/
│   ├── TreeDiagramSpec.java          Layer 1 input
│   ├── TreeDiagramLayout.java        Layer 1 output
│   └── TreeLayoutCalculator.java     Layer 1 algorithm (extracted from TreeLayout)

studio.fantasyit.ether_craft.recipe.factory.render.widget/   (NEW)
├── TreeDiagramWidget.java            Layer 2 top-level compositor
├── TreeDiagramViewport.java          pan/zoom/clamp state + screen↔world transforms
├── ViewportInputHandler.java         WASD, drag, scroll input processing
├── EdgeBatchRenderer.java            L-shaped edge line drawing
├── NodeRenderer.java                 @FunctionalInterface: render a node
├── NodeTooltipProvider.java          @FunctionalInterface: tooltip for a node
└── NodeClickHandler.java             @FunctionalInterface: click callback

studio.fantasyit.ether_craft.integration.jei/
├── JEITreeSlottedWidget.java         (NEW) Layer 3a: JEI adapter
├── TreeLayout.java                   (MODIFY) delegate to TreeLayoutCalculator; keep compat
├── ViewportTransform.java            (DEPRECATE → remove)
└── ViewportSlotProxy.java            (DEPRECATE → remove)

studio.fantasyit.ether_craft.menu.factory/
└── EtherProcessFactoryScreen.java    (MODIFY, future) Layer 3b integration
```

## Layer 1: Data Layer

**Package**: `studio.fantasyit.ether_craft.recipe.factory.render.data`

**Dependencies**: Only `java.*`. No Minecraft, NeoForge, or JEI types.

### Input: `TreeDiagramSpec`

```java
public record TreeDiagramSpec(
    List<NodeSpec> nodes,
    OutputSpec output,
    LayoutConfig config
) {
    public record NodeSpec(
        String id,
        int subSlotCount,         // 1 for input nodes, N for process nodes
        @Nullable String nextId,  // null = connects to output
        boolean isInput
    ) {}

    public record OutputSpec(int slotCount) {}

    public record LayoutConfig(
        int slotSize,         // default 18
        int outputSlotSize,   // default 22
        int viewWidth,        // default 140
        int viewHeight,       // default 90
        int padding,          // default 4
        int nodeGap,          // default 15
        int minSpacing        // default 26
    ) {}
}
```

A static factory `TreeDiagramSpec.fromJson(EtherProcessRecipeJson)` bridges from recipe JSON to the abstract spec.

### Output: `TreeDiagramLayout`

```java
public final class TreeDiagramLayout {
    public final List<PositionedNode> nodes;
    public final List<Edge> edges;
    public final PositionedOutput output;
    public final int canvasWidth;
    public final int canvasHeight;

    public record PositionedNode(
        String id,
        int x, int y,           // top-left corner in world coords
        int width, int height,  // bounding rect
        int midY,               // Y midpoint (used for edge routing)
        int exitX               // right-edge X (used for edge routing)
    ) {}

    public record PositionedOutput(
        int x, int y, int width, int height, int midY
    ) {}

    public record Edge(int fromX, int fromY, int toX, int toY) {}
}
```

### Algorithm: `TreeLayoutCalculator`

```java
public final class TreeLayoutCalculator {
    public static TreeDiagramLayout compute(TreeDiagramSpec spec) {
        // Extracted from TreeLayout.computeLevels() + coordinate assignment logic.
        // Steps:
        // 1. computeLevels(): BFS backward from leaves (nextId==null → level 1).
        //    Predecessors that point to a node get level+1.
        // 2. Per-node height = subSlotCount * slotSize + (subSlotCount-1) * gap.
        // 3. Group by level; sort level-1 alphabetically, others by next-node Y.
        // 4. Assign X by column (level N → rightmost column, level 1 → leftmost).
        //    canvasWidth = padding + maxLevel * spacing + outputWidth + padding.
        // 5. Output at right edge.
        // 6. Compute Edge: exitX → next-node left edge or output left edge.
    }
}
```

### Bridge: `TreeLayout` compatibility

```java
// In TreeLayout.java (integration/jei/):
public static TreeLayout compute(EtherProcessRecipeJson json) {
    TreeDiagramSpec spec = TreeDiagramSpec.fromJson(json);
    TreeDiagramLayout layout = TreeLayoutCalculator.compute(spec);
    return new TreeLayout(layout, json); // reconstruct legacy records
}
```

This keeps existing `EtherProcessCategory` code working while migrating.

## Layer 2: Widget Layer

**Package**: `studio.fantasyit.ether_craft.recipe.factory.render.widget`

**Dependencies**: `GuiGraphicsExtractor`, `Matrix3x2fStack`, `Font`, `Component`, `ItemStack`, GLFW constants. No JEI.

### `TreeDiagramViewport`

```java
public class TreeDiagramViewport {
    double panX, panY;
    double zoom = 1.0;
    double minZoom;
    int canvasWidth, canvasHeight;
    int viewWidth, viewHeight;

    // screen → world coordinate transform
    double toWorldX(double screenX)   // screenX → worldX
    double toWorldY(double screenY)
    // world → screen coordinate transform
    double toScreenX(double worldX)
    double toScreenY(double worldY)

    void clampPan()   // panX/panY clamped so canvas always fills viewport
    void zoomAt(double screenX, double screenY, double factor)
}
```

Logic extracted from `ViewportTransform.clampPan()` and `zoomAt()`.

### `ViewportInputHandler`

```java
public class ViewportInputHandler {
    // Interprets raw GLFW events + mouse state against a Viewport.
    // State: boolean dragging, double lastMouseX, lastMouseY.

    boolean handleKey(int keyCode)       // WASD pan, +/- zoom
    boolean handleMouseClicked(double mx, double my, int button, boolean isSimulate)
    boolean handleMouseDragged(double mx, double my, int button, double dragX, double dragY)
    boolean handleMouseScrolled(double mx, double my, double scrollY)
}
```

### `EdgeBatchRenderer`

```java
public final class EdgeBatchRenderer {
    // Draws L-shaped polylines using GuiGraphicsExtractor.horizontalLine()/verticalLine().
    // The gui pose already has the viewport transform applied.

    static void render(GuiGraphicsExtractor g, List<TreeDiagramLayout.Edge> edges, int lineColor)
}
```

### `NodeRenderer<T>`, `NodeTooltipProvider<T>`, `NodeClickHandler<T>`

```java
@FunctionalInterface
public interface NodeRenderer<T> {
    void render(GuiGraphicsExtractor g, TreeDiagramLayout.PositionedNode node, T data,
                double mouseWorldX, double mouseWorldY);
}

@FunctionalInterface
public interface NodeTooltipProvider<T> {
    @Nullable List<Component> getTooltip(T data, TreeDiagramLayout.PositionedNode node);
}

@FunctionalInterface
public interface NodeClickHandler<T> {
    void onClick(T data, TreeDiagramLayout.PositionedNode node, int button);
}
```

### `TreeDiagramWidget`

```java
public class TreeDiagramWidget {
    final TreeDiagramViewport viewport;
    final ViewportInputHandler input;
    final EdgeBatchRenderer edgeRenderer;
    TreeDiagramLayout layout;

    // Per-node bindings: id → (renderer, tooltip, click, data)
    Map<String, Binding<?>> bindings;

    record Binding<T>(NodeRenderer<T> renderer, NodeTooltipProvider<T> tooltip,
                       NodeClickHandler<T> click, T data) {}

    // --- Construction ---
    TreeDiagramWidget(int viewX, int viewY, int viewW, int viewH)

    void setLayout(TreeDiagramLayout layout)
    <T> void bindNode(String id, NodeRenderer<T> r, NodeTooltipProvider<T> t,
                       NodeClickHandler<T> c, T data)

    // --- Render ---
    void render(GuiGraphicsExtractor g, Font font, double mouseX, double mouseY)
    //  1. enableScissor(viewX, viewY, viewW, viewH)
    //  2. pose.pushMatrix → translate(viewport) → scale(zoom)
    //  3. edgeRenderer.render(edges)
    //  4. for each node: binding.renderer.render(g, node, data, worldMX, worldMY)
    //  5. pose.popMatrix → disableScissor
    //  6. If hovering a node with tooltip: g.setTooltipForNextFrame(...)

    // --- Input (forwarded by Screen) ---
    boolean mouseClicked(double mx, double my, int button)
    boolean mouseDragged(double mx, double my, int button, double dx, double dy)
    boolean mouseReleased(double mx, double my, int button)
    boolean mouseScrolled(double mx, double my, double scrollX, double scrollY)
    boolean keyPressed(int keyCode)
}
```

### Screen Integration Pattern

```java
public class SomeScreen extends AbstractContainerScreen<?> {
    TreeDiagramWidget treeWidget;

    @Override protected void init() {
        super.init();
        treeWidget = new TreeDiagramWidget(left + x, top + y, 140, 90);
        treeWidget.setLayout(layout);
        treeWidget.bindNode("input_1", myRenderer, myTooltip, myClick, dataObj);
    }

    @Override public boolean mouseClicked(double x, double y, int b) {
        return treeWidget.mouseClicked(x, y, b) || super.mouseClicked(x, y, b);
    }
    @Override public boolean mouseDragged(double x, double y, int b, double dx, double dy) {
        return treeWidget.mouseDragged(x, y, b, dx, dy) || super.mouseDragged(x, y, b, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int b) {
        return treeWidget.mouseReleased(x, y, b) || super.mouseReleased(x, y, b);
    }
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        return treeWidget.mouseScrolled(mx, my, sx, sy) || super.mouseScrolled(mx, my, sx, sy);
    }
    @Override public boolean keyPressed(int kc, int sc, int m) {
        return treeWidget.keyPressed(kc) || super.keyPressed(kc, sc, m);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        super.extractRenderState(g, mx, my, a);
        treeWidget.render(g, font, mx, my);
    }
}
```

## Layer 3: Adapters

### 3a. JEI Adapter (`JEITreeSlottedWidget`)

```java
public class JEITreeSlottedWidget implements ISlottedRecipeWidget, IJeiInputHandler {
    TreeDiagramViewport viewport;
    ViewportInputHandler input;
    EdgeBatchRenderer edgeRenderer;
    TreeDiagramLayout layout;
    List<IRecipeSlotDrawable> allSlots;
    Map<String, IRecipeSlotDrawable> slotById;   // nodeId → JEI slot

    // drawWidget(): apply viewport transform → draw edges → draw all slots
    // getSlotUnderMouse(): inverse-transform mouse → hit-test against slotById values
    // handleInput(): delegate to input (with isSimulate awareness)
    // handleMouseScrolled(): delegate to input
    // handleMouseDragged(): delegate to input
}
```

Usage in `EtherProcessCategory.createRecipeExtras()`:

```java
TreeDiagramLayout layout = TreeLayoutCalculator.compute(TreeDiagramSpec.fromJson(recipe.json));

JEITreeSlottedWidget widget = new JEITreeSlottedWidget(layout);
for (PositionedNode node : layout.nodes) {
    IRecipeSlotDrawable slot = createJeiSlotFor(node); // via builder
    widget.registerSlot(node.id, slot);
}
builder.addSlottedWidget(widget, widget.allSlots);
builder.addInputHandler(widget);
```

### 3b. Factory GUI (Future)

Per-node `NodeRenderer<ChipRenderData>` that calls:
- `GuiGraphicsExtractor.item(chipStack, x, y)` for item rendering
- `GuiGraphicsExtractor.fill()` for ether progress bar overlays
- `GuiGraphicsExtractor.fill()` for path direction indicators

### 3c. Standalone Browser (Future)

Full-screen `TreeDiagramScreen` with recipe listing sidebar, same `TreeDiagramWidget`.

## Migration Plan

| Phase | Scope | Impact |
|-------|-------|--------|
| **Phase 1** | Create `recipe/factory/render/data/` + `recipe/factory/render/widget/` | New files, zero existing code changes |
| **Phase 2** | Modify `TreeLayout.compute()` to delegate to `TreeLayoutCalculator` | Legacy API preserved |
| **Phase 3** | Create `JEITreeSlottedWidget`, switch `EtherProcessCategory` to use it | JEI behavior identical, internal refactor |
| **Phase 4** | Remove `ViewportTransform`, `ViewportSlotProxy` | Cleanup |
| **Phase 5** | Integrate into `EtherProcessFactoryScreen` | New feature |
| **Phase 6** | Standalone browser screen | New feature |

Phases 1-4 are pure refactoring (no user-facing change). Phases 5-6 are new features.

## API Surface

### Minecraft API (MC 26.1.2)

| Old (removed) | New |
|---------------|-----|
| `GuiGraphics` | `GuiGraphicsExtractor` |
| `PoseStack` / `pushPose()` / `popPose()` | `Matrix3x2fStack` / `.pushMatrix()` / `.popMatrix()` |
| `RenderSystem.enableScissor()` | `GuiGraphicsExtractor.enableScissor()` |
| `GuiGraphics.fill()` | `GuiGraphicsExtractor.fill()` |
| `GuiGraphics.renderItem()` | `GuiGraphicsExtractor.item()` |
| `GuiGraphics.renderTooltip()` | `GuiGraphicsExtractor.setTooltipForNextFrame()` |
| `GuiGraphics.drawString()` | `GuiGraphicsExtractor.text()` |
| `Screen.mouseClicked(double,double,int)` | Same (no change) |
| `Screen.mouseDragged(double,double,int,double,double)` | Same (no change) |

### Layer Dependencies

| Layer | Imports |
|-------|---------|
| Layer 1 (data) | `java.util.*` only |
| Layer 2 (widget) | `GuiGraphicsExtractor`, `Matrix3x2fStack`, `Font`, `Component`, `ItemStack`, GLFW |
| Layer 3 (JEI) | `ISlottedRecipeWidget`, `IJeiInputHandler`, `IRecipeSlotDrawable` + Layer 2 |
| Layer 3 (Factory) | `EtherProcessFactoryEntity`, `GuiGraphicsExtractor` + Layer 2 |
