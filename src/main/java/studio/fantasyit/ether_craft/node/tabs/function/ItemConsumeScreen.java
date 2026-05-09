package studio.fantasyit.ether_craft.node.tabs.function;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.node.filter.FilterGuiRegClient;
import studio.fantasyit.ether_craft.node.plugins.function.AbstractItemConsumeFunction;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;
import studio.fantasyit.ether_craft.util.UIUtil;

public class ItemConsumeScreen extends BaseEtherNodeTabWidgetProvider<AbstractItemConsumeFunction> {
    public ItemConsumeScreen(AbstractItemConsumeFunction menuContext, EtherAdaptNodeScreen screen) {
        super(menuContext, screen);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        UIUtil.nineSliced(graphics, EtherAdaptNodeAsset.INFO_PANEL, lx(93), ly(15), 75, 48, 1);
    }

    @Override
    public void createWidget() {
        super.createWidget();
        FilterGuiRegClient.widget(screen,context.filter.whitelist);
    }
}
