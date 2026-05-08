package studio.fantasyit.ether_craft.node.tabs;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;
import studio.fantasyit.ether_craft.menu.base.ImageAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.node.AbstractNodePlugin;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseEtherNodeTabWidgetProvider<T extends AbstractNodePlugin> {
    public static final int WIDGET_POSITION_X = 20;
    public static final int WIDGET_POSITION_Y = 20;
    protected T context;
    protected EtherAdaptNodeScreen screen;

    protected record IARec(ImageAsset asset, int x, int y) {
    }

    protected List<IARec> imageAssets = new ArrayList<>();

    public BaseEtherNodeTabWidgetProvider(T menuContext, EtherAdaptNodeScreen screen) {
        this.context = menuContext;
        this.screen = screen;
    }

    public void collectImageAsset(ImageAsset asset, int x, int y) {
        imageAssets.add(new IARec(asset, x, y));
    }

    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }

    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        for (Slot slot : screen.getMenu().toDrawSlot) {
            EtherAdaptNodeAsset.SLOT.blit(graphics, lx(slot.x) - 1, ly(slot.y) - 1);
        }
        for (IARec iar : imageAssets) {
            iar.asset.blit(graphics, lx(iar.x), ly(iar.y));
        }
    }

    public void createWidget() {

    }

    public void tick() {
    }

    public int lx(int x) {
        return screen.getLeftPos() + x;
    }

    public int ly(int y) {
        return screen.getTopPos() + y;
    }
}
