package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import studio.fantasyit.ether_craft.stream.client.ClientVESHData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record EtherStreamSetDyingS2C(
        PosDir posDir,
        List<StreamEntry> entries
) implements CustomPacketPayload {

    public record StreamEntry(
            int streamId,
            int tickCount,
            int ether,
            @Nullable Component label,
            int labelColor
    ) {
    }

    public static final Type<@NotNull EtherStreamSetDyingS2C> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "ether_stream_update")
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, PosDir> POSDIR_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            PosDir::pos,
            Direction.STREAM_CODEC,
            PosDir::dir,
            PosDir::new
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, @Nullable Component> NULLABLE_COMPONENT_CODEC =
            ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC.map(
                    opt -> opt.orElse(null),
                    Optional::ofNullable
            );

    private static final StreamCodec<RegistryFriendlyByteBuf, StreamEntry> STREAM_ENTRY_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StreamEntry::streamId,
            ByteBufCodecs.VAR_INT, StreamEntry::tickCount,
            ByteBufCodecs.VAR_INT, StreamEntry::ether,
            NULLABLE_COMPONENT_CODEC, StreamEntry::label,
            ByteBufCodecs.INT, StreamEntry::labelColor,
            StreamEntry::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull EtherStreamSetDyingS2C> CODEC = StreamCodec.composite(
            POSDIR_CODEC, EtherStreamSetDyingS2C::posDir,
            ByteBufCodecs.collection(ArrayList::new, STREAM_ENTRY_CODEC), EtherStreamSetDyingS2C::entries,
            EtherStreamSetDyingS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientVESHData.get().handleDying(this);
        });
    }
}
