package studio.fantasyit.ether_craft.node.plugins;

import studio.fantasyit.ether_craft.entity.EtherStreamEntity;

public interface IEtherStreamCapabilityProviderPlugin {
    void provideCapabilities(EtherStreamEntity entity);
}
