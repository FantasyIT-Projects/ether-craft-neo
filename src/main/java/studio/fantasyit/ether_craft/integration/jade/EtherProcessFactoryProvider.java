package studio.fantasyit.ether_craft.integration.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryBlock;

public enum EtherProcessFactoryProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final Identifier UID = EtherCraft.id("ether_process_factory");
    static final String KEY_ETHER = "ether";
    static final String KEY_PRESSURE = "pressure";
    static final String KEY_LEAK = "leak";

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        int level = accessor.getBlockState().getValueOrElse(EtherProcessFactoryBlock.LEVEL, 1);
        tooltip.add(Component.translatable("jade.ether_craft.adapt_node.level", level));

        CompoundTag data = accessor.getServerData();
        if (data.contains(KEY_ETHER)) {
            tooltip.add(Component.translatable("jade.ether_craft.process_factory.ether", data.getLong(KEY_ETHER).orElse(0L)));
        }
        if (data.contains(KEY_PRESSURE)) {
            tooltip.add(Component.translatable("jade.ether_craft.process_factory.pressure", data.getInt(KEY_PRESSURE).orElse(0)));
        }
        if (data.contains(KEY_LEAK)) {
            tooltip.add(Component.translatable("jade.ether_craft.process_factory.leak", data.getInt(KEY_LEAK).orElse(0)));
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
