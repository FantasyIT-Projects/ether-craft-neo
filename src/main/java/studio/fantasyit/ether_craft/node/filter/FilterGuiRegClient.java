package studio.fantasyit.ether_craft.node.filter;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import studio.fantasyit.ether_craft.menu.base.widget.IASwitchButton;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.menu.node.ScreenMenuSyncer;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

import java.util.function.Supplier;

public class FilterGuiRegClient {

    public static void widget(EtherAdaptNodeScreen screen, Supplier<Boolean> vGetter, InstalledPlugin plugin) {
        IASwitchButton iaSwitchButton = screen.addRenderableWidget(new IASwitchButton(
                screen.getLeftPos() + 15, screen.getTopPos() + 77,
                EtherAdaptNodeAsset.BTN_BLACK,
                EtherAdaptNodeAsset.BTN_BLACK_HOVER,
                EtherAdaptNodeAsset.BTN_WHITE,
                EtherAdaptNodeAsset.BTN_WHITE_HOVER,
                null,
                Component.translatable("ether_craft.gui.node.filter.using_black_list"),
                Component.translatable("ether_craft.gui.node.filter.using_white_list"),
                t -> FilterGuiRegClient.useWhitelist(plugin, t)
        ));
        iaSwitchButton.setDown(vGetter.get());
        screen.registerMenuSyncer(new ScreenMenuSyncer<>(vGetter, iaSwitchButton::setDown));
    }

    private static Boolean useWhitelist(InstalledPlugin plugin, Boolean aBoolean) {
        ClientPacketDistributor.sendToServer(new SyncScreenDataC2S(plugin, FilterGuiRegCommon.SYNC_FILTER, 0, aBoolean ? 0 : 1));
        return true;
    }
}
