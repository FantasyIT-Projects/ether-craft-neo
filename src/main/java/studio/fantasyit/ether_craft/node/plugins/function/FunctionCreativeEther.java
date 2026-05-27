package studio.fantasyit.ether_craft.node.plugins.function;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;

public class FunctionCreativeEther extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("generator/creative");
    public static final Identifier ID_FUNC = EtherCraft.id("generator/creative_f");

    public FunctionCreativeEther(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void tickWork() {
        nodeEntity.setEther(nodeEntity.getMaxEther());
    }
}
