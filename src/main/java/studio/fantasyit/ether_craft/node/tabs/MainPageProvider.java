package studio.fantasyit.ether_craft.node.tabs;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;
import studio.fantasyit.ether_craft.menu.base.RangeLimitPlaceContainer;
import studio.fantasyit.ether_craft.menu.base.RangeLimitSlot;
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

        for (Slot s : screen.getMenu().slots) {
            if (s instanceof RangeLimitSlot rls && !rls.valid()) {
                graphics.fill(lx(s.x - 1), ly(s.y - 1), lx(s.x + 17), ly(s.y + 17), 0x60000000);
            }
        }
        RangeLimitPlaceContainer normalStorage = screen.getMenu().entity.normalStorage;
        int accessibleCount = normalStorage.getAccessibleCount();
        if (accessibleCount < normalStorage.getContainerSize() - 7) {
            EtherAdaptNodeAsset.LOCK.blit(graphics, lx(83), ly(78) + 18 * Math.ceilDiv(accessibleCount, 7));
        } else {
            int lastX = 7 + (accessibleCount % 7) * 18;
            EtherAdaptNodeAsset.LOCK.blit(graphics, lx(lastX + 186 - EtherAdaptNodeAsset.LOCK.w / 2), ly(114));
        }
    }
}
