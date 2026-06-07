package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;

public record PlatingTriggerC2S(Identifier effectId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull PlatingTriggerC2S> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "plating_trigger")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull PlatingTriggerC2S> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC,
            PlatingTriggerC2S::effectId,
            PlatingTriggerC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
