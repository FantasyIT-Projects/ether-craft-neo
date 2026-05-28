package studio.fantasyit.ether_craft.node.plugins.base;

import studio.fantasyit.ether_craft.entity.stream.EtherStreamEntity;

public interface IEtherStreamCapabilityProviderPlugin {
    void provideCapabilities(EtherStreamEntity entity);
}
