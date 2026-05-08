package studio.fantasyit.ether_craft.node.filter;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import studio.fantasyit.ether_craft.menu.base.btn.IASwitchButton;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;

public class FilterGuiRegClient {

    public static void widget(EtherAdaptNodeScreen screen,boolean defv) {
        IASwitchButton iaSwitchButton = screen.addRenderableWidget(new IASwitchButton(
                screen.getLeftPos() + 15, screen.getTopPos() + 77,
                EtherAdaptNodeAsset.BTN_BLACK,
                EtherAdaptNodeAsset.BTN_BLACK_HOVER,
                EtherAdaptNodeAsset.BTN_WHITE,
                EtherAdaptNodeAsset.BTN_WHITE_HOVER,
                null,
                Component.translatable("ether_craft.gui.node.filter.using_black_list"),
                Component.translatable("ether_craft.gui.node.filter.using_white_list"),
                FilterGuiRegClient::useWhitelist
        ));
        iaSwitchButton.setDown(defv);
    }

    private static Boolean useWhitelist(Boolean aBoolean) {
        ClientPacketDistributor.sendToServer(new SyncScreenDataC2S(FilterGuiRegCommon.SYNC_FILTER, 0, aBoolean ? 1 : 0));
        return true;
    }
}
