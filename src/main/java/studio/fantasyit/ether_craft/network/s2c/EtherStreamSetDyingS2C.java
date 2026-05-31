package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHDataGetter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record EtherStreamSetDyingS2C(
        PosDir posDir,
        List<Integer> entries
) implements CustomPacketPayload {
    public static final Type<@NotNull EtherStreamSetDyingS2C> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "ether_stream_set_dying")
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, @Nullable Component> NULLABLE_COMPONENT_CODEC =
            ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC.map(
                    opt -> opt.orElse(null),
                    Optional::ofNullable
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull EtherStreamSetDyingS2C> CODEC = StreamCodec.composite(
            PosDir.STREAM_CODEC, EtherStreamSetDyingS2C::posDir,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.INT), EtherStreamSetDyingS2C::entries,
            EtherStreamSetDyingS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientVESHDataGetter.get().handleDying(this);
        });
    }
}
