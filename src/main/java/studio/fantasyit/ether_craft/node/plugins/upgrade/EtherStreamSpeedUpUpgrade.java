package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;

public class EtherStreamSpeedUpUpgrade extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("ether_stream_speed_up_upgrade");

    public EtherStreamSpeedUpUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }
}
