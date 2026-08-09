package studio.fantasyit.ether_craft.node.plugins.base;

import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;

public interface ISyncScreenDataPlugin {
    void syncScreenData(SyncScreenDataC2S message);
}
