package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;

public class RedstoneSwitchUpgrade extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("redstone_switch");
    public static final Identifier ID_REVERT = EtherCraft.id("redstone_switch_revert");

    public final boolean workWithSignal;

    public RedstoneSwitchUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId, boolean workWithSignal) {
        super(nodeEntity, installedId);
        this.workWithSignal = workWithSignal;
    }

    @Override
    public boolean preTick() {
        if (nodeEntity.getLevel() == null) return true;
        boolean hasSignal = nodeEntity.getCachedNeighborSignal();
        return workWithSignal ? hasSignal : !hasSignal;
    }
}
