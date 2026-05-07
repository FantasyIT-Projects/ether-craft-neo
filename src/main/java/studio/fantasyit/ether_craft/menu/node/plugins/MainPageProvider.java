package studio.fantasyit.ether_craft.menu.node.plugins;

import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.node.plugins.MainPageDummyPlugin;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;

public class MainPageProvider extends BaseEtherNodeTabWidgetProvider<MainPageDummyPlugin> {
    public MainPageProvider(MainPageDummyPlugin menuContext, EtherAdaptNodeScreen screen) {
        super(menuContext, screen);
    }

    @Override
    public void createWidget() {

    }
}
