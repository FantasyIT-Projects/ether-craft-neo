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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class ViewportTransform implements ISlottedRecipeWidget, IJeiInputHandler {
    record SlotInfo(IRecipeSlotDrawable slot, int worldX, int worldY) {}

    private static final int LINE_COLOR = 0xFFAAAAAA;
    private static final int VIEW_W = 140;
    private static final int VIEW_H = 90;

    double panX, panY;
    double zoom = 1.0;
    final double minZoom;
    final int canvasWidth, canvasHeight;
    final List<SlotInfo> slots;
    final List<TreeLayout.Edge> edges;
    boolean dragging;

    ViewportTransform(int canvasWidth, int canvasHeight,
                      List<IRecipeSlotDrawable> rawSlots,
                      List<Integer> worldXs, List<Integer> worldYs,
                      List<TreeLayout.Edge> edges) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.edges = edges;
        this.minZoom = Math.min(1.0, Math.min(
                (double) VIEW_W / canvasWidth,
                (double) VIEW_H / canvasHeight));

        this.slots = new ArrayList<>(rawSlots.size());
        for (int i = 0; i < rawSlots.size(); i++) {
            ViewportSlotProxy proxy = new ViewportSlotProxy(
                    rawSlots.get(i),
                    () -> panX,
                    () -> panY,
                    () -> zoom
            );
            proxy.setPosition(worldXs.get(i), worldYs.get(i));
            this.slots.add(new SlotInfo(proxy, worldXs.get(i), worldYs.get(i)));
        }
    }

    void clampPan() {
        double lowerX = Math.min(0, VIEW_W - canvasWidth * zoom);
        double upperX = Math.max(0, VIEW_W - canvasWidth * zoom);
        double lowerY = Math.min(0, VIEW_H - canvasHeight * zoom);
        double upperY = Math.max(0, VIEW_H - canvasHeight * zoom);
        panX = Math.clamp(panX, lowerX, upperX);
        panY = Math.clamp(panY, lowerY, upperY);
    }

    private void zoomAt(double screenX, double screenY, double factor) {
        double oldZoom = zoom;
        double newZoom = Math.clamp(zoom * factor, minZoom, 1.0);
        if (newZoom == oldZoom) return;
        double ratio = newZoom / oldZoom;
        panX = screenX - (screenX - panX) * ratio;
        panY = screenY - (screenY - panY) * ratio;
        zoom = newZoom;
        clampPan();
    }

    private boolean isOverSlot(double mouseX, double mouseY) {
        double worldMX = (mouseX - panX) / zoom;
        double worldMY = (mouseY - panY) / zoom;
        for (SlotInfo s : slots) {
            if (s.slot.isMouseOver(worldMX, worldMY)) return true;
        }
        return false;
    }

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
        graphics.pose().translate((float) panX, (float) panY);
        graphics.pose().scale((float) zoom, (float) zoom);

        for (TreeLayout.Edge edge : edges) {
            drawLine(graphics, edge.fromX(), edge.fromY(), edge.toX(), edge.toY());
        }

        for (SlotInfo s : slots) {
            s.slot.draw(graphics);
        }

        graphics.pose().popMatrix();
        graphics.disableScissor();
    }

    @Override
    public Optional<RecipeSlotUnderMouse> getSlotUnderMouse(double mouseX, double mouseY) {
        double worldMX = (mouseX - panX) / zoom;
        double worldMY = (mouseY - panY) / zoom;
        for (SlotInfo s : slots) {
            if (s.slot.isMouseOver(worldMX, worldMY)) {
                return Optional.of(new RecipeSlotUnderMouse(s.slot, getPosition()));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
        int key = input.getKey().getValue();

        if (input.getKey().getType() == InputConstants.Type.KEYSYM) {
            double step = 20.0;
            boolean handled = switch (key) {
                case GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP -> {
                    panY += step;
                    yield true;
                }
                case GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> {
                    panY -= step;
                    yield true;
                }
                case GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT -> {
                    panX += step;
                    yield true;
                }
                case GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT -> {
                    panX -= step;
                    yield true;
                }
                case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> {
                    zoomAt(VIEW_W / 2.0, VIEW_H / 2.0, 1.1);
                    yield true;
                }
                case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> {
                    zoomAt(VIEW_W / 2.0, VIEW_H / 2.0, 1.0 / 1.1);
                    yield true;
                }
                default -> false;
            };
            if (handled && !input.isSimulate()) {
                clampPan();
            }
            return handled;
        }

        if (input.getKey().getType() == InputConstants.Type.MOUSE
                && input.getKey().getValue() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (input.isSimulate()) {
                dragging = !isOverSlot(mouseX, mouseY);
                return dragging;
            }
            dragging = false;
            return false;
        }

        return false;
    }

    @Override
    public boolean handleMouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (scrollDeltaY != 0) {
            double factor = scrollDeltaY > 0 ? 1.1 : 1.0 / 1.1;
            zoomAt(mouseX, mouseY, factor);
            return true;
        }
        return false;
    }

    @Override
    public boolean handleMouseDragged(double mouseX, double mouseY, InputConstants.Key mouseKey,
                                      double dragX, double dragY) {
        if (dragging && mouseKey.getValue() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            panX += dragX;
            panY += dragY;
            clampPan();
            return true;
        }
        return false;
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
}
