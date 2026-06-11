package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;

public record PlatingKeyTriggerC2S() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull PlatingKeyTriggerC2S> TYPE = new CustomPacketPayload.Type<>(
            EtherCraft.id("plating_key_trigger")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull PlatingKeyTriggerC2S> CODEC = StreamCodec.unit(new PlatingKeyTriggerC2S());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
