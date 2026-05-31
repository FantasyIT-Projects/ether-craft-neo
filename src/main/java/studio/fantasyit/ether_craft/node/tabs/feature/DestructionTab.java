package studio.fantasyit.ether_craft.node.tabs.feature;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import studio.fantasyit.ether_craft.menu.base.widget.IASwitchButton;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.menu.node.ScreenMenuSyncer;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.filter.FilterGuiRegClient;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.node.plugins.feature.DestructionUpgrade;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;

public class DestructionTab extends BaseEtherNodeTabWidgetProvider<DestructionUpgrade> {
    public DestructionTab(PluginMenuContext<DestructionUpgrade> context, EtherAdaptNodeScreen screen) {
        super(context, screen);
    }

    @Override
    public void createWidget() {
        FilterGuiRegClient.widget(screen, () -> plugin.filter.whitelist, plugin.installedId);

        IASwitchButton modeButton = new IASwitchButton(
                lx(90), ly(12),
                EtherAdaptNodeAsset.BTN_BLANK,
                EtherAdaptNodeAsset.BTN_BLANK_HOVER,
                EtherAdaptNodeAsset.BTN_BLANK_DOWN,
                EtherAdaptNodeAsset.BTN_BLANK_DOWN_HOVER,
                null,
                Component.translatable("ether_craft.gui.node.destruction.mode_overflow"),
                Component.translatable("ether_craft.gui.node.destruction.mode_all"),
                t -> {
                    ClientPacketDistributor.sendToServer(new SyncScreenDataC2S(
                            plugin.installedId,
                            DestructionUpgrade.SYNC_MODE,
                            0, (!t) ? 1 : 0));
                    return true;
                }
        );
        modeButton.setDown(plugin.destroyMode == DestructionUpgrade.DestroyMode.ALL);
        screen.addRenderableWidget(modeButton);
        screen.registerMenuSyncer(new ScreenMenuSyncer<>(
                () -> plugin.destroyMode == DestructionUpgrade.DestroyMode.ALL,
                modeButton::setDown));
    }
}
