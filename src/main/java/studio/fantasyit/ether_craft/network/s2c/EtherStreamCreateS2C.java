package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.EtherConsumer;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.client.ClientVESHData;
import studio.fantasyit.ether_craft.stream.client.ClientVESHDataGetter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record EtherStreamCreateS2C(
        PosDir posDir,
        List<StreamEntry> entries
) implements CustomPacketPayload {

    public record StreamEntry(
            int streamId,
            Vec3 startPos,
            Vec3 motion,
            int ether,
            int tickCount,
            EtherConsumer.State consumerState,
            @Nullable Component label,
            int labelColor
    ) {
    }

    public static final Type<@NotNull EtherStreamCreateS2C> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "ether_stream_create")
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeDouble(v.x);
                buf.writeDouble(v.y);
                buf.writeDouble(v.z);
            },
            buf -> new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, @Nullable Component> NULLABLE_COMPONENT_CODEC =
            ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC.map(
                    opt -> opt.orElse(null),
                    Optional::ofNullable
            );

    private static final StreamCodec<RegistryFriendlyByteBuf, StreamEntry> STREAM_ENTRY_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StreamEntry::streamId,
            VEC3_CODEC, StreamEntry::startPos,
            VEC3_CODEC, StreamEntry::motion,
            ByteBufCodecs.VAR_INT, StreamEntry::ether,
            ByteBufCodecs.VAR_INT, StreamEntry::tickCount,
            EtherConsumer.State.STREAM_CODEC, StreamEntry::consumerState,
            NULLABLE_COMPONENT_CODEC, StreamEntry::label,
            ByteBufCodecs.INT, StreamEntry::labelColor,
            StreamEntry::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull EtherStreamCreateS2C> CODEC = StreamCodec.composite(
            PosDir.STREAM_CODEC, EtherStreamCreateS2C::posDir,
            ByteBufCodecs.collection(ArrayList::new, STREAM_ENTRY_CODEC), EtherStreamCreateS2C::entries,
            EtherStreamCreateS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientVESHDataGetter.get().handleCreate(this);
        });
    }
}
