package studio.fantasyit.ether_craft.node.plugins.base;

import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.data.StreamExtraProperty;

public interface IEtherStreamCapabilityProviderPlugin {
    void provideCapabilities(IEtherStreamLike entity, StreamExtraProperty extraProperty);
}
