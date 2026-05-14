package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.entity.EtherStreamEntity;
import studio.fantasyit.ether_craft.node.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.IEtherStreamCapabilityProviderPlugin;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.stream.EtherStreamStorageCapability;

import java.util.Optional;

public class EtherStreamStorageUpgrade extends AbstractNodePlugin implements IEtherStreamCapabilityProviderPlugin {
    public static final Identifier ID = EtherCraft.id("ether_stream_storage_upgrade");

    public EtherStreamStorageUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void provideCapabilities(EtherStreamEntity entity) {
        Optional<studio.fantasyit.ether_craft.stream.IStreamCapability> existing = entity.getCapability(EtherStreamStorageCapability.ID);
        if (existing.isPresent()) {
            ((EtherStreamStorageCapability) existing.get()).addSlots(2);
        } else {
            entity.addCapability(new EtherStreamStorageCapability(2));
        }
    }
}
