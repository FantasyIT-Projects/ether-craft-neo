package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.IEtherStreamCapabilityProviderPlugin;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.stream.cap.EtherStreamBreakBlockCapability;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;

import java.util.Optional;

public class EtherStreamBreakBlockUpgrade extends AbstractNodePlugin implements IEtherStreamCapabilityProviderPlugin {
    public static final Identifier ID = EtherCraft.id("block_breaker_upgrade");

    public EtherStreamBreakBlockUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    private ItemStack getTool() {
        return nodeEntity.featureUpgradeStorage.getItem(installedId.id());
    }

    @Override
    public void provideCapabilities(IEtherStreamLike entity) {
        ItemStack tool = getTool();
        if (tool.isEmpty()) return;

        Optional<IStreamCapability> existing = entity.getCapability(EtherStreamBreakBlockCapability.ID);
        if (existing.isPresent() && existing.get() instanceof EtherStreamBreakBlockCapability breakBlock) {
            breakBlock.addTool(tool);
        } else {
            EtherStreamBreakBlockCapability cap = new EtherStreamBreakBlockCapability();
            cap.addTool(tool);
            entity.addCapability(cap);
        }
    }
}
