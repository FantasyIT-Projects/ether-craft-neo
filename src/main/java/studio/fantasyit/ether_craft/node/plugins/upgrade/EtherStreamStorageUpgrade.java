package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.entity.stream.EtherStreamEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.IEtherStreamCapabilityProviderPlugin;
import studio.fantasyit.ether_craft.stream.EtherStreamStorageCapability;

import java.util.Optional;

public class EtherStreamStorageUpgrade extends AbstractNodePlugin implements IEtherStreamCapabilityProviderPlugin {
    public static final Identifier ID = EtherCraft.id("ether_stream_storage_upgrade");
    public static final Identifier ID_1 = EtherCraft.id("ether_stream_storage_upgrade_1");
    public static final Identifier ID_2 = EtherCraft.id("ether_stream_storage_upgrade_2");
    int streamIncrease;

    public EtherStreamStorageUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId, int streamIncrease) {
        super(nodeEntity, installedId);
        this.streamIncrease = streamIncrease;
    }

    @Override
    public void provideCapabilities(EtherStreamEntity entity) {
        Optional<studio.fantasyit.ether_craft.stream.IStreamCapability> existing = entity.getCapability(EtherStreamStorageCapability.ID);
        if (existing.isPresent()) {
            ((EtherStreamStorageCapability) existing.get()).addSlots(this.streamIncrease);
        } else {
            entity.addCapability(new EtherStreamStorageCapability(this.streamIncrease));
        }
    }
}
