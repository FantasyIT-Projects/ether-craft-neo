package studio.fantasyit.ether_craft.menu.base.btn;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.menu.base.ImageAsset;

import java.util.function.Consumer;
import java.util.function.Function;

public class IASwitchButton extends AbstractWidget {
    private final ImageAsset mainNormal;
    private final ImageAsset mainHover;
    private final ImageAsset downNormal;
    private final ImageAsset downHover;
    private final @Nullable ImageAsset icon;

    private final Component mainMessage;
    private final Component downMessage;
    private final Function<Boolean,Boolean> onClick;
    private boolean down;

    public IASwitchButton(int x, int y, ImageAsset mainNormal, ImageAsset mainHover, ImageAsset downNormal, ImageAsset downHover, @Nullable ImageAsset icon, Component mainMessage, Component downMessage, Function<Boolean,Boolean> onClick) {
        super(x, y, mainNormal.w, mainNormal.h, mainMessage);
        this.setTooltip(Tooltip.create(downMessage));
        this.mainNormal = mainNormal;
        this.mainHover = mainHover;
        this.downNormal = downNormal;
        this.downHover = downHover;
        this.icon = icon;
        this.down = true;
        this.mainMessage = mainMessage;
        this.downMessage = downMessage;
        this.onClick = onClick;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.down) {
            if (this.isHoveredOrFocused()) {
                this.downHover.blit(graphics, this.getX(), this.getY());
            } else {
                this.downNormal.blit(graphics, this.getX(), this.getY());
            }
        } else {
            if (this.isHoveredOrFocused()) {
                this.mainHover.blit(graphics, this.getX(), this.getY());
            } else {
                this.mainNormal.blit(graphics, this.getX(), this.getY());
            }
        }
        if (icon != null) {
            icon.blit(graphics, this.getX(), this.getY());
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if(this.onClick.apply(this.down)){
            this.setDown(!this.down);
            this.playDownSound(Minecraft.getInstance().getSoundManager());
        }
    }

    public void setDown(boolean b) {
        this.down = b;
        this.setTooltip(Tooltip.create(this.down ? this.downMessage : this.mainMessage));
    }

    public boolean isDown() {

        return this.down;
    }
}
