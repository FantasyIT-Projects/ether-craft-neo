package studio.fantasyit.ether_craft.node.tabs;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.node.plugins.MainPageDummyPlugin;

public class MainPageProvider extends BaseEtherNodeTabWidgetProvider<MainPageDummyPlugin> {
    public MainPageProvider(MainPageDummyPlugin menuContext, EtherAdaptNodeScreen screen) {
        super(menuContext, screen);
        collectImageAsset(EtherAdaptNodeAsset.SLOT_ETHER, 26, 17);
        collectImageAsset(EtherAdaptNodeAsset.SLOT_LARGE, 26, 43);
        collectImageAsset(EtherAdaptNodeAsset.ETHER_BAR_CTR, 26, 38);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        EtherAdaptNodeAsset.getEtherBarIA(
                screen.getMenu().entity.getEther(),
                screen.getMenu().entity.getMaxEther()
        ).blit(graphics, lx(27), ly(39));
    }
}
