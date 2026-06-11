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
