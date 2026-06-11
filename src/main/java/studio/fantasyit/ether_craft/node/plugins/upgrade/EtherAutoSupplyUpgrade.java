package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;

public class EtherAutoSupplyUpgrade extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("ether_auto_supply_upgrade");

    public EtherAutoSupplyUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void tickWork() {
        int count = 0;
        for (int i = 0; i < nodeEntity.featureUpgradeStorage.getContainerSize(); i++) {
            if (nodeEntity.featureUpgradeStorage.getPlugin(i) instanceof EtherAutoSupplyUpgrade)
                count++;
        }
        if (count > 1)
            return;

        if (nodeEntity.getEther() < Config.etherAutoSupplyThreshold)
            nodeEntity.receiveEther(Config.etherAutoSupplyEtherPerTick);
    }
}
