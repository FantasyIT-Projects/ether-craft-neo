package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.IEtherStreamCapabilityProviderPlugin;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.cap.EtherStreamCarryEntityCapability;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;

import java.util.Optional;

public class EtherStreamCarryEntityUpgrade extends AbstractNodePlugin implements IEtherStreamCapabilityProviderPlugin {
    public static final Identifier ID = EtherCraft.id("carry_entity_upgrade");
    public static final Identifier ID_PLAYER = EtherCraft.id("carry_player_upgrade");

    public EtherStreamCarryEntityUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void provideCapabilities(IEtherStreamLike entity) {
        ItemStack item = nodeEntity.featureUpgradeStorage.getItem(installedId.id());
        if (item.isEmpty()) return;

        boolean playerOnly = ID_PLAYER.equals(installedId.pluginId());
        Identifier capId = playerOnly ? EtherStreamCarryEntityCapability.ID_PLAYER : EtherStreamCarryEntityCapability.ID;
        Optional<IStreamCapability> existing = entity.getCapability(capId);
        if (existing.isEmpty()) {
            entity.addCapability(new EtherStreamCarryEntityCapability(nodeEntity.getBlockPos(), playerOnly));
        }
    }
}
