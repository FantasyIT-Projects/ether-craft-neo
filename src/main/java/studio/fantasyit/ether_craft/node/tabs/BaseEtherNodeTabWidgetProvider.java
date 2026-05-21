package studio.fantasyit.ether_craft.node.tabs;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import oshi.util.tuples.Pair;
import studio.fantasyit.ether_craft.menu.base.ImageAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class BaseEtherNodeTabWidgetProvider<T extends AbstractNodePlugin> {
    public static final int WIDGET_POSITION_X = 20;
    public static final int WIDGET_POSITION_Y = 20;
    protected final PluginMenuContext<T> context;
    protected final T plugin;
    protected EtherAdaptNodeScreen screen;

    protected record IARec(ImageAsset asset, int x, int y) {
    }

    protected List<IARec> imageAssets = new ArrayList<>();
    protected List<Pair<Rect2i, Supplier<List<Component>>>> tooltipAreas = new ArrayList<>();

    public BaseEtherNodeTabWidgetProvider(PluginMenuContext<T> context, EtherAdaptNodeScreen screen) {
        this.context = context;
        this.plugin = context.plugin;
        this.screen = screen;
    }

    public void collectImageAsset(ImageAsset asset, int x, int y) {
        imageAssets.add(new IARec(asset, x, y));
    }

    public void collectTooltipArea(Rect2i area, Supplier<List<Component>> tooltip) {
        tooltipAreas.add(new Pair<>(area, tooltip));
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

    public void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (Pair<Rect2i, Supplier<List<Component>>> tooltipArea : tooltipAreas) {
            if (tooltipArea.getA().contains(mouseX, mouseY)) {
                List<Component> tooltip = tooltipArea.getB().get();
                if (tooltip != null) {
                    graphics.setTooltipForNextFrame(tooltip.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
                }
            }
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
