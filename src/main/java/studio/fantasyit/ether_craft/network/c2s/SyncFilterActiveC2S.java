package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;

public record SyncFilterActiveC2S(boolean active) implements CustomPacketPayload {
    public static final Type<@NotNull SyncFilterActiveC2S> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(
                    EtherCraft.MODID, "sync_filter_active"
            )
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull SyncFilterActiveC2S> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            SyncFilterActiveC2S::active,
            SyncFilterActiveC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
