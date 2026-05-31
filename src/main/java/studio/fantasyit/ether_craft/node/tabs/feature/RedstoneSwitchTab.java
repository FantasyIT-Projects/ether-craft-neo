package studio.fantasyit.ether_craft.node.tabs.feature;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import studio.fantasyit.ether_craft.menu.base.widget.IASwitchButton;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.menu.node.ScreenMenuSyncer;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.node.plugins.feature.RedstoneSwitchUpgrade;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;

public class RedstoneSwitchTab extends BaseEtherNodeTabWidgetProvider<RedstoneSwitchUpgrade> {
    public RedstoneSwitchTab(PluginMenuContext<RedstoneSwitchUpgrade> context, EtherAdaptNodeScreen screen) {
        super(context, screen);
        screen.registerMenuSyncer(new ScreenMenuSyncer<>(() -> plugin.workWithSignal, v -> {
        }));
    }

    @Override
    public void createWidget() {
        IASwitchButton button = new IASwitchButton(
                lx(15), ly(77),
                EtherAdaptNodeAsset.BTN_BLANK,
                EtherAdaptNodeAsset.BTN_BLANK_HOVER,
                EtherAdaptNodeAsset.BTN_BLANK_DOWN,
                EtherAdaptNodeAsset.BTN_BLANK_DOWN_HOVER,
                null,
                Component.translatable("ether_craft.gui.node.redstone_switch.work_with_signal"),
                Component.translatable("ether_craft.gui.node.redstone_switch.work_without_signal"),
                t -> {
                    ClientPacketDistributor.sendToServer(new SyncScreenDataC2S(
                            plugin.installedId,
                            RedstoneSwitchUpgrade.SYNC_MODE,
                            0,
                            (!t) ? 1 : 0
                    ));
                    return true;
                }
        );
        button.setDown(plugin.workWithSignal);
        screen.addRenderableWidget(button);
        screen.registerMenuSyncer(new ScreenMenuSyncer<>(() -> plugin.workWithSignal, button::setDown));
    }
}
