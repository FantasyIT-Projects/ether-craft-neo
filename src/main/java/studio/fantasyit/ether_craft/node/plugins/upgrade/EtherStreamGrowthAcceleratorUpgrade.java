package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.IEtherStreamCapabilityProviderPlugin;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.cap.EtherStreamGrowthAcceleratorCapability;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;

import java.util.Optional;

public class EtherStreamGrowthAcceleratorUpgrade extends AbstractNodePlugin implements IEtherStreamCapabilityProviderPlugin {
    public static final Identifier ID = EtherCraft.id("growth_accelerator_upgrade");
    public static final Identifier ID_ALL = EtherCraft.id("growth_accelerator_upgrade_allow_all");

    public EtherStreamGrowthAcceleratorUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void provideCapabilities(IEtherStreamLike entity) {
        boolean allowAll = ID_ALL.equals(installedId.pluginId());
        Optional<IStreamCapability> existing = entity.getCapability(allowAll ? EtherStreamGrowthAcceleratorCapability.ID_ALL : EtherStreamGrowthAcceleratorCapability.ID);
        if (existing.isEmpty()) {
            entity.addCapability(new EtherStreamGrowthAcceleratorCapability(allowAll));
        }
    }
}
