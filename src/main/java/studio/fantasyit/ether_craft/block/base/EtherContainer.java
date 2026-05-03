package studio.fantasyit.ether_craft.block.base;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.network.PacketDistributor;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.network.s2c.SyncBlockEtherValueS2C;
import studio.fantasyit.ether_craft.register.BlockAttachmentDataRegistry;

import java.util.Optional;

public interface EtherContainer {
    BlockCapability<EtherContainer, Void> ETHER_CONTAINER =
            BlockCapability.createVoid(
                    EtherCraft.id("ether_container"),
                    EtherContainer.class
            );

    default BlockEntity be() {
        return (BlockEntity) this;
    }

    default long getEther() {
        return Optional.ofNullable(be().getExistingDataOrNull(BlockAttachmentDataRegistry.ETHER_CONTAINER)).orElse(0L);
    }

    default void setEther(long amount) {
        long o = getEther();
        be().setData(BlockAttachmentDataRegistry.ETHER_CONTAINER, amount);
        if(o != amount)
            syncClient();
    }

    default void receiveEther(long amount) {
        setEther(getEther() + amount);
    }

    default long extractEther(long amount) {
        long extracted = Math.min(getEther(), amount);
        setEther(getEther() - extracted);
        return extracted;
    }

    default void syncClient() {
        BlockEntity be = be();
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        PacketDistributor.sendToAllPlayers(
                new SyncBlockEtherValueS2C(
                        getEther(),
                        be.getBlockPos(),
                        be.getLevel().dimension().identifier()
                )
        );
    }
}
