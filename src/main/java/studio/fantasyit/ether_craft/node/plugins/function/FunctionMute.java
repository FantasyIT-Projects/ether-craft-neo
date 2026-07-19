package studio.fantasyit.ether_craft.node.plugins.function;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

public class FunctionMute extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("mute");

    public FunctionMute(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void tickWork() {
        if (nodeEntity.getLevel() == null)
            return;
        var muteSources = nodeEntity.getLevel().getData(AttachmentDataRegistry.LEVEL_MUTE_SOURCE);
        if (nodeEntity.getEther() < Config.nodeMuteEtherCostPreTick)
            return;

        nodeEntity.extractEther(Config.nodeMuteEtherCostPreTick);
        muteSources.notifyBlock(nodeEntity.getLevel(), nodeEntity.getBlockPos());
    }
}
