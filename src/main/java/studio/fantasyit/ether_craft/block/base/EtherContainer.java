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

    default long getMaxEther() {
        return 0;
    }

    default long validateMax(long amount) {
        long max = getMaxEther();
        if (max == 0 || amount <= max)
            return amount;
        return max;
    }

    default void setEther(long amount) {
        long o = getEther();
        amount = validateMax(amount);
        setEtherNoUpdate(amount);
        if (o != amount)
            syncClient();
    }

    default void setEtherNoUpdate(long amount) {
        amount = validateMax(amount);
        be().setData(BlockAttachmentDataRegistry.ETHER_CONTAINER, amount);
    }

    default void receiveEther(long amount) {
        setEther(getEther() + amount);
    }

    default void receiveEtherNoUpdate(long amount) {
        setEtherNoUpdate(getEther() + amount);
    }

    default long extractEther(long amount) {
        long extracted = Math.min(getEther(), amount);
        setEther(getEther() - extracted);
        return extracted;
    }

    default long extractEtherNoUpdate(long amount) {
        long extracted = Math.min(getEther(), amount);
        setEtherNoUpdate(getEther() - extracted);
        return extracted;
    }

    default long getCanReceive(long amount) {
        return Math.max(0, validateMax(amount + getEther()) - getEther());
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
