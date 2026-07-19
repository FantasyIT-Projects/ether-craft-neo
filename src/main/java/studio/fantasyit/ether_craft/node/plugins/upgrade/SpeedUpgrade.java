package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;

public class SpeedUpgrade extends AbstractNodePlugin implements IGeneratorAdjuster {
    public static Identifier ID = EtherCraft.id("speed_upgrade");

    public SpeedUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }
    @Override
    public IGeneratorAdjuster.AdjustedParameters adjust(IGeneratorAdjuster.AdjustedParameters adjustedParameters) {
        return new AdjustedParameters(Math.ceilDiv(adjustedParameters.burnTicks(), 2), adjustedParameters.preTick() * 2);
    }
}
