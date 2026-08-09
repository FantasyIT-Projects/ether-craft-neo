package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.NodeProperty;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.IModifyNodePropertyPlugin;

public class EtherItemifyUpgrade extends AbstractNodePlugin implements IModifyNodePropertyPlugin {
    public static final Identifier ID = EtherCraft.id("ether_itemify_upgrade");

    public EtherItemifyUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void modifyNodeProperty(NodeProperty nodeProperty) {
        nodeProperty.itemifyEther = true;
    }
}