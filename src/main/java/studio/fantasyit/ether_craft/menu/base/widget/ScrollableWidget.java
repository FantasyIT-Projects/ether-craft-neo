package studio.fantasyit.ether_craft.menu.base.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import studio.fantasyit.ether_craft.menu.base.ImageAsset;

import java.util.function.Consumer;

public class ScrollableWidget extends AbstractWidget {
    private final int maxValue;
    private final ImageAsset background;
    private final ImageAsset block;
    private final ImageAsset blockHover;
    private final Consumer<Integer> onValueChange;
    int value;
    boolean isDragging;
    int startDragValue;
    double startDragPos;

    public ScrollableWidget(int x, int y, int maxValue, ImageAsset background, ImageAsset block, ImageAsset blockHover, ImageAsset blockDown, Consumer<Integer> onValueChange) {
        super(x, y, block.w, background.h, Component.empty());
        this.background = background;
        this.block = block;
        this.blockHover = blockHover;
        this.maxValue = maxValue;
        this.value = 0;
        this.isDragging = false;
        this.onValueChange = onValueChange;
    }

    public void setValue(int value) {
        if (isDragging) return;
        this.value = Math.clamp(value, 0, maxValue);
    }

    public int getValue() {
        return value;
    }

    public int getBlockTop() {
        return (background.h - block.h) * value / maxValue;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (event.y() - getY() >= getBlockTop() && event.y() - getY() <= getBlockTop() + background.h) {
            this.isDragging = true;
            this.startDragPos = event.y();
            this.startDragValue = value;
        }
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        this.isDragging = false;
        this.onValueChange.accept(value);
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        super.onDrag(event, dx, dy);
        if (this.isDragging) {
            this.value = Math.clamp((int) (this.startDragValue - (this.startDragPos - event.y()) / (background.h - block.h) * maxValue), 0, maxValue);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int v = (scrollY > 0 ? 1 : -1);
        if (Minecraft.getInstance().hasControlDown())
            v *= 10;
        if (Minecraft.getInstance().hasShiftDown())
            v *= 10;
        this.value = Math.clamp(this.value - v, 0, maxValue);
        this.onValueChange.accept(value);
        return true;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        background.blit(graphics, getX() + (block.w - background.w) / 2, getY());
        block.blit(graphics, getX(), getY() + getBlockTop());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

    }

}
