package studio.fantasyit.ether_craft.integration.jade;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;

public enum EtherAdaptNodeServerDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof EtherAdaptNodeEntity be) {
            data.putLong(EtherAdaptNodeProvider.KEY_ETHER, be.getEther());
            data.putLong(EtherAdaptNodeProvider.KEY_MAX_ETHER, be.getMaxEther());

            int count = 0;
            if (!be.functionStorage.getItem(0).isEmpty()) {
                Identifier id = BuiltInRegistries.ITEM.getKey(be.functionStorage.getItem(0).getItem());
                data.putString(EtherAdaptNodeProvider.KEY_PLUGIN_PREFIX + count, id.toString());
                count++;
            }
            int upgradeSlots = be.getUpgradeCount();
            for (int i = 0; i < upgradeSlots; i++) {
                ItemStack stack = be.featureUpgradeStorage.getItem(i);
                if (!stack.isEmpty()) {
                    Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    data.putString(EtherAdaptNodeProvider.KEY_PLUGIN_PREFIX + count, id.toString());
                    count++;
                }
            }
            data.putInt(EtherAdaptNodeProvider.KEY_PLUGIN_COUNT, count);
        }
    }

    @Override
    public Identifier getUid() {
        return EtherAdaptNodeProvider.INSTANCE.getUid();
    }
}
