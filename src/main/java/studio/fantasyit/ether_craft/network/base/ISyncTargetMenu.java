package studio.fantasyit.ether_craft.network.base;

import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;

public interface ISyncTargetMenu {
    void syncScreenData(SyncScreenDataC2S message);
}
