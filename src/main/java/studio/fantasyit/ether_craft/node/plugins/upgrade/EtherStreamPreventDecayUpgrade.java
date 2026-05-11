package studio.fantasyit.ether_craft.node.plugins.upgrade;

import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.NodeProperty;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

public class EtherStreamPreventDecayUpgrade extends AbstractNodePlugin {
    public EtherStreamPreventDecayUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void modifyNodeProperty(NodeProperty nodeProperty) {
        nodeProperty.streamPreventDecay++;
    }
}
