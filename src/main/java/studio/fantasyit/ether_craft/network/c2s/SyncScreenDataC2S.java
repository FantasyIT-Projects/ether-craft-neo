package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;

public record SyncScreenDataC2S(Identifier id, int index, int data) implements CustomPacketPayload {
    public static final Type<@NotNull SyncScreenDataC2S> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(
                    EtherCraft.MODID, "sync_screen_data"
            )
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull SyncScreenDataC2S> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC,
            SyncScreenDataC2S::id,
            ByteBufCodecs.INT,
            SyncScreenDataC2S::index,
            ByteBufCodecs.INT,
            SyncScreenDataC2S::data,
            SyncScreenDataC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
