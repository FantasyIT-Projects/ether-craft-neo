package studio.fantasyit.ether_craft.block.base;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.network.PacketDistributor;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.network.s2c.SyncBlockEtherValueS2C;

public interface EtherContainer {
    BlockCapability<EtherContainer, Void> ETHER_CONTAINER =
            BlockCapability.createVoid(
                    EtherCraft.id("ether_container"),
                    EtherContainer.class
            );

    default BlockEntity be() {
        return (BlockEntity) this;
    }

    long getEther();

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

    void setEtherNoUpdate(long amount);

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

    default boolean shouldSync() {
        return false;
    }

    default void syncClient() {
        if (!shouldSync()) return;
        BlockEntity be = be();
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        PacketDistributor.sendToPlayersTrackingChunk(
                (ServerLevel) be.getLevel(),
                ChunkPos.containing(be.getBlockPos()),
                new SyncBlockEtherValueS2C(
                        getEther(),
                        be.getBlockPos()
                )
        );
    }
}
