package studio.fantasyit.ether_craft.node.plugins.base;

import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

public interface IEtherStreamCapabilityProviderPlugin {
    void provideCapabilities(IEtherStreamLike entity);
}
