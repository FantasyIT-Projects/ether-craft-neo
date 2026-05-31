package studio.fantasyit.ether_craft.stream.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

public interface IEtherStreamSyncedData {
    Identifier getId();

    void toBuffer(FriendlyByteBuf writer);

    @FunctionalInterface
    interface Builder {
        IEtherStreamSyncedData build(FriendlyByteBuf reader);
    }
}
