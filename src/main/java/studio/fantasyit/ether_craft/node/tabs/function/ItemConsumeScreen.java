package studio.fantasyit.ether_craft.node.tabs.function;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.node.filter.FilterGuiRegClient;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.node.plugins.function.AbstractItemConsumeFunction;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;
import studio.fantasyit.ether_craft.util.UIUtil;

public class ItemConsumeScreen extends BaseEtherNodeTabWidgetProvider<AbstractItemConsumeFunction> {

    public ItemConsumeScreen(PluginMenuContext<AbstractItemConsumeFunction> context, EtherAdaptNodeScreen screen) {
        super(context, screen);
        collectImageAsset(EtherAdaptNodeAsset.ETHER_BAR_CTR, 26, 38);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        UIUtil.nineSliced(graphics, EtherAdaptNodeAsset.INFO_PANEL, lx(93), ly(15), 75, 48, 1);
        if (screen.getMenu().entity != null)
            UIUtil.renderEtherBarProgress(
                    screen.getMenu().entity.getEther(),
                    screen.getMenu().entity.getMaxEther(),
                    lx(27), ly(39), EtherAdaptNodeAsset.ETHER_BAR_CTR.w - 2, 2, graphics
            );
    }

    @Override
    public void createWidget() {
        super.createWidget();
        FilterGuiRegClient.widget(screen, ()->plugin.filter.whitelist, AbstractItemConsumeFunction.FILTER_PREFIX);
    }
}
