package studio.fantasyit.ether_craft.menu.factory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import studio.fantasyit.ether_craft.menu.base.ImageAsset;

import java.util.function.Consumer;

/**
 * 工厂 GUI 右侧"树形可视化"小面板中的开关按钮。
 *
 * <p>图标用 {@code graphics.fill} 现场绘制一棵迷你树，不需要新增贴图资源。
 */
public class FactoryTreeButton extends AbstractWidget {
    private final ImageAsset normal;
    private final ImageAsset hover;
    private final ImageAsset downNormal;
    private final ImageAsset downHover;
    private final Consumer<Boolean> onToggle;
    private boolean down;

    public FactoryTreeButton(int x, int y, ImageAsset normal, ImageAsset hover,
                             ImageAsset downNormal, ImageAsset downHover,
                             Component message, Consumer<Boolean> onToggle) {
        super(x, y, normal.w, normal.h, message);
        this.normal = normal;
        this.hover = hover;
        this.downNormal = downNormal;
        this.downHover = downHover;
        this.onToggle = onToggle;
        this.setTooltip(Tooltip.create(message));
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (down) {
            (isHoveredOrFocused() ? downHover : downNormal).blit(graphics, getX(), getY());
        } else {
            (isHoveredOrFocused() ? hover : normal).blit(graphics, getX(), getY());
        }
        drawGlyph(graphics);
    }

    private void drawGlyph(GuiGraphicsExtractor graphics) {
        int x = getX();
        int y = getY();
        int color = down ? 0xFFFFFFFF : 0xFF44546A;
        // 右侧输出方块
        graphics.fill(x + 9, y + 6, x + 13, y + 10, color);
        // 主干
        graphics.fill(x + 7, y + 4, x + 9, y + 12, color);
        graphics.fill(x + 4, y + 4, x + 8, y + 5, color);
        graphics.fill(x + 4, y + 11, x + 8, y + 12, color);
        // 左侧两个输入/工序方块
        graphics.fill(x + 2, y + 3, x + 5, y + 6, color);
        graphics.fill(x + 2, y + 10, x + 5, y + 13, color);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        setDown(!down);
        if (onToggle != null) onToggle.accept(down);
        this.playDownSound(Minecraft.getInstance().getSoundManager());
    }

    public void setDown(boolean down) {
        this.down = down;
    }

    public boolean isDown() {
        return down;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
