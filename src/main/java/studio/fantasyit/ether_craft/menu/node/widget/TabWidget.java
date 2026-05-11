package studio.fantasyit.ether_craft.menu.node.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.menu.base.ImageAsset;

public class TabWidget extends AbstractWidget {
    public static final Identifier BACKGROUND = EtherCraft.id("textures/gui/ether_adapt_node_main.png");
    private final ItemStack icon;
    private final boolean isActivate;
    private static final ImageAsset TAB_ACTIVATED = ImageAsset.from4Point(BACKGROUND, 176, 0, 199, 23);
    private static final ImageAsset TAB_INACTIVATED = ImageAsset.from4Point(BACKGROUND, 200, 0, 223, 20);
    private final Runnable onPress;

    public TabWidget(int x, int y, Component message, ItemStack icon, boolean isActivate, Runnable onPress) {
        super(x, y, TAB_ACTIVATED.w, TAB_ACTIVATED.h, message);
        this.icon = icon;
        this.isActivate = isActivate;
        this.setTooltip(Tooltip.create(message));
        this.onPress = onPress;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (isActivate) {
            TAB_ACTIVATED.blit(graphics, getX(), getY());
        } else {
            TAB_INACTIVATED.blit(graphics, getX(), getY());
        }
        graphics.item(icon, getX() + 4, getY() + 4);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onPress.run();
        this.playDownSound(Minecraft.getInstance().getSoundManager());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
