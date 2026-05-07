package studio.fantasyit.ether_craft.node.tabs;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.node.AbstractNodePlugin;

public abstract class BaseEtherNodeTabWidgetProvider<T extends AbstractNodePlugin> {
    public static final int WIDGET_POSITION_X = 20;
    public static final int WIDGET_POSITION_Y = 20;
    protected T context;
    protected EtherAdaptNodeScreen screen;

    public BaseEtherNodeTabWidgetProvider(T menuContext, EtherAdaptNodeScreen screen) {
        this.context = menuContext;
        this.screen = screen;
    }
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }
    public abstract void createWidget();
    public void tick() {
    }
}
