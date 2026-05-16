package studio.fantasyit.ether_craft.menu.base.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import studio.fantasyit.ether_craft.menu.base.ImageAsset;

public class NamePencilButton extends AbstractWidget {
    private final EditBox linkedEditBox;
    private final ImageAsset pencilOn;
    private final ImageAsset pencilOff;

    public NamePencilButton(int x, int y, EditBox linkedEditBox, ImageAsset pencilOn, ImageAsset pencilOff) {
        super(x, y, pencilOff.w, pencilOff.h, Component.empty());
        this.linkedEditBox = linkedEditBox;
        this.pencilOn = pencilOn;
        this.pencilOff = pencilOff;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        linkedEditBox.setFocused(true);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        (linkedEditBox.isFocused() ? pencilOn : pencilOff).blit(graphics, getX(), getY());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
