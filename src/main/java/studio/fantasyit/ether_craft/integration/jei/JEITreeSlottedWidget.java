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
    private static final int VIEW_W = TreeLayout.WIDTH;
    private static final int VIEW_H = TreeLayout.HEIGHT;

    private final TreeDiagramViewport viewport;
    private final ViewportInputHandler inputHandler;
    private final TreeDiagramLayout layout;
    private final List<IRecipeSlotDrawable> allSlots;
    private final List<IRecipeSlotDrawable> proxiedSlots;
    private final Map<String, IRecipeSlotDrawable> slotById = new HashMap<>();

    public JEITreeSlottedWidget(TreeDiagramLayout layout,
                                 List<IRecipeSlotDrawable> allSlots) {
        this.layout = layout;
        this.allSlots = new ArrayList<>(allSlots);
        this.viewport = new TreeDiagramViewport(layout.canvasWidth, layout.canvasHeight, VIEW_W, VIEW_H);
        this.inputHandler = new ViewportInputHandler(viewport);
        viewport.centerPan();
        this.proxiedSlots = new ArrayList<>(allSlots.size());
        for (var slot : allSlots) {
            this.proxiedSlots.add(new ViewportSlotProxy(slot, viewport));
        }
    }

    public void registerSlot(String nodeId, IRecipeSlotDrawable slot) {
        int idx = allSlots.indexOf(slot);
        if (idx >= 0 && idx < proxiedSlots.size()) {
            slotById.put(nodeId, proxiedSlots.get(idx));
        }
    }

    public List<IRecipeSlotDrawable> getAllSlots() {
        return proxiedSlots;
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
        graphics.pose().translate((float) viewport.getPanX(), (float) viewport.getPanY());
        graphics.pose().scale((float) viewport.getZoom(), (float) viewport.getZoom());

        EdgeBatchRenderer.render(graphics, layout.edges);

        for (var slot : proxiedSlots) {
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
