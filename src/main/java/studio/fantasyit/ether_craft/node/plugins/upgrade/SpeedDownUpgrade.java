package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;

public class SpeedDownUpgrade extends AbstractNodePlugin implements IGeneratorAdjuster {
    public static Identifier ID = EtherCraft.id("speed_down_upgrade");

    public SpeedDownUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public AdjustedParameters adjust(AdjustedParameters adjustedParameters) {
        return new IGeneratorAdjuster.AdjustedParameters(adjustedParameters.burnTicks() * 2, Math.ceilDiv(adjustedParameters.preTick(), 2));
    }
}
