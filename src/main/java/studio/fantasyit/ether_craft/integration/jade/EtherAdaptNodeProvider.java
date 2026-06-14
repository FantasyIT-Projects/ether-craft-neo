package studio.fantasyit.ether_craft.integration.jade;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeBlock;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;

public enum EtherAdaptNodeProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final Identifier UID = EtherCraft.id("ether_adapt_node");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        int level = accessor.getBlockState().getValueOrElse(EtherAdaptNodeBlock.LEVEL, 1);
        tooltip.add(Component.translatable("jade.ether_craft.adapt_node.level", level));

        if (accessor.getBlockEntity() instanceof EtherAdaptNodeEntity be) {
            long ether = be.getEther();
            long maxEther = be.getMaxEther();
            tooltip.add(Component.translatable("jade.ether_craft.adapt_node.ether", ether, maxEther));

            int plugins = be.getUpgradeCount();
            tooltip.add(Component.translatable("jade.ether_craft.adapt_node.plugins", plugins));
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
