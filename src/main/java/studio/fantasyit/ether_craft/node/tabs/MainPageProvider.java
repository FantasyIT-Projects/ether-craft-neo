package studio.fantasyit.ether_craft.node.tabs;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import studio.fantasyit.ether_craft.menu.base.RangeLimitPlaceContainer;
import studio.fantasyit.ether_craft.menu.base.slot.RangeLimitSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.node.plugins.MainPageDummyPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.util.UIUtil;

import java.util.List;

public class MainPageProvider extends BaseEtherNodeTabWidgetProvider<MainPageDummyPlugin> {

    public MainPageProvider(PluginMenuContext<MainPageDummyPlugin> context, EtherAdaptNodeScreen screen) {
        super(context, screen);
        collectImageAsset(EtherAdaptNodeAsset.SLOT_LARGE, 26, 43);
        collectImageAsset(EtherAdaptNodeAsset.ETHER_BAR_CTR, 26, 38);
        collectImageAsset(EtherAdaptNodeAsset.HAMMER, 48, 45);
        collectImageAsset(EtherAdaptNodeAsset.ICON_UPGRADE, 148, 51);
        collectTooltipArea(new Rect2i(lx(26), ly(38), EtherAdaptNodeAsset.ETHER_BAR_CTR.w, EtherAdaptNodeAsset.ETHER_BAR_CTR.h),
                () -> List.of(Component.translatable("menu.ether_craft.ether_bar_tooltip", screen.getMenu().entity.getEther()))
        );
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        if (screen.getMenu().entity.etherStorage.isEmpty())
            EtherAdaptNodeAsset.SLOT_ETHER.blit(graphics, lx(26), ly(17));
        else
            EtherAdaptNodeAsset.SLOT_LARGE.blit(graphics, lx(26), ly(17));
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        UIUtil.renderEtherBarProgress(
                screen.getMenu().entity.getEther(),
                screen.getMenu().entity.getMaxEther(),
                lx(27), ly(39), EtherAdaptNodeAsset.ETHER_BAR_CTR.w - 2, 2, graphics
        );
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
