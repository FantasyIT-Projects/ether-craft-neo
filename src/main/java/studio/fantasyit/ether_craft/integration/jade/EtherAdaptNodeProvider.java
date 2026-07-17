package studio.fantasyit.ether_craft.integration.jade;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeBlock;

public enum EtherAdaptNodeProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final Identifier UID = EtherCraft.id("ether_adapt_node");
    static final String KEY_ETHER = "ether";
    static final String KEY_MAX_ETHER = "maxEther";
    static final String KEY_PLUGIN_COUNT = "pluginCount";
    static final String KEY_PLUGIN_PREFIX = "plugin_";

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        int level = accessor.getBlockState().getValueOrElse(EtherAdaptNodeBlock.LEVEL, 1);
        tooltip.add(Component.translatable("jade.ether_craft.adapt_node.level", level));

        CompoundTag data = accessor.getServerData();
        if (data.contains(KEY_ETHER)) {
            long ether = data.getLong(KEY_ETHER).orElse(0L);
            long maxEther = data.getLong(KEY_MAX_ETHER).orElse(0L);
            tooltip.add(Component.translatable("jade.ether_craft.adapt_node.ether", ether, maxEther));
        }

        int pluginCount = data.getInt(KEY_PLUGIN_COUNT).orElse(0);
        if (pluginCount > 0) {
            tooltip.add(Component.translatable("jade.ether_craft.adapt_node.plugin_list"));
            for (int i = 0; i < pluginCount; i++) {
                String idStr = data.getString(KEY_PLUGIN_PREFIX + i).orElse("");
                if (!idStr.isEmpty()) {
                    ItemStack item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(idStr))
                            .map(ItemStack::new)
                            .orElse(ItemStack.EMPTY);
                    if (!item.isEmpty()) {
                        tooltip.add(Component.literal("  - ").append(item.getHoverName()));
                    }
                }
            }
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
