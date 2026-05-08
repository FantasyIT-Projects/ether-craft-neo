package studio.fantasyit.ether_craft.network;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import studio.fantasyit.ether_craft.network.c2s.SetFilterSlotC2S;
import studio.fantasyit.ether_craft.network.c2s.SetFilterSlotC2SHandler;
import studio.fantasyit.ether_craft.network.c2s.TriggerSwitchTabC2S;
import studio.fantasyit.ether_craft.network.c2s.TriggerSwitchTabC2SHandler;

public class NetworkClient {
    public static void clientMsg(PayloadRegistrar event) {
        event.playToServer(
                TriggerSwitchTabC2S.TYPE,
                TriggerSwitchTabC2S.CODEC,
                (d, c) -> TriggerSwitchTabC2SHandler.handle(d, c.player())
        );
        event.playToServer(
                SetFilterSlotC2S.TYPE,
                SetFilterSlotC2S.CODEC,
                (d, c) -> SetFilterSlotC2SHandler.handle(d, c.player())
        );
    }
}
