package studio.fantasyit.ether_craft.integration.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;

public enum EtherProcessFactoryServerDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof EtherProcessFactoryEntity be) {
            data.putLong(EtherProcessFactoryProvider.KEY_ETHER, be.getEther());
            data.putInt(EtherProcessFactoryProvider.KEY_PRESSURE, be.pressureBonus);
            data.putInt(EtherProcessFactoryProvider.KEY_LEAK, be.leak);
        }
    }

    @Override
    public Identifier getUid() {
        return EtherProcessFactoryProvider.INSTANCE.getUid();
    }
}
