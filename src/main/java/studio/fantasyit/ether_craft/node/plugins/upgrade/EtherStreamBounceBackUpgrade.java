package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.IEtherStreamCapabilityProviderPlugin;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.cap.EtherStreamBounceBackCapability;

public class EtherStreamBounceBackUpgrade extends AbstractNodePlugin implements IEtherStreamCapabilityProviderPlugin {
    public static final Identifier ID = EtherCraft.id("ether_stream_bounce_back_upgrade");

    public EtherStreamBounceBackUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void provideCapabilities(IEtherStreamLike entity) {
        if (entity.getCapability(EtherStreamBounceBackCapability.ID).isEmpty()) {
            entity.addCapability(new EtherStreamBounceBackCapability());
        }
    }
}
