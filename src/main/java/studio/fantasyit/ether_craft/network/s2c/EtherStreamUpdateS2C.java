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
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.event.ClientVESHData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record EtherStreamUpdateS2C(
        PosDir posDir,
        List<StreamEntry> entries
) implements CustomPacketPayload {
    public record StreamEntry(
            int streamId,
            int tickCount,
            int ether,
            byte flags,
            int deathTick,
            @Nullable Component label,
            int labelColor
    ) {
        public static final byte FLAG_DEAD = 1;
        public static final byte FLAG_DYING = 2;

        public boolean isDead() {
            return (flags & FLAG_DEAD) != 0;
        }

        public boolean isDying() {
            return (flags & FLAG_DYING) != 0;
        }

        private static final StreamCodec<RegistryFriendlyByteBuf, @Nullable Component> NULLABLE_COMPONENT_CODEC =
                ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC.map(
                        opt -> opt.orElse(null),
                        Optional::ofNullable
                );

        static final StreamCodec<RegistryFriendlyByteBuf, StreamEntry> STREAM_ENTRY_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, StreamEntry::streamId,
                ByteBufCodecs.VAR_INT, StreamEntry::tickCount,
                ByteBufCodecs.VAR_INT, StreamEntry::ether,
                ByteBufCodecs.BYTE, StreamEntry::flags,
                ByteBufCodecs.VAR_INT, StreamEntry::deathTick,
                NULLABLE_COMPONENT_CODEC, StreamEntry::label,
                ByteBufCodecs.INT, StreamEntry::labelColor,
                StreamEntry::new
        );
    }

    public static final Type<@NotNull EtherStreamUpdateS2C> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "ether_stream_update")
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, PosDir> POSDIR_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            PosDir::pos,
            Direction.STREAM_CODEC,
            PosDir::dir,
            PosDir::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull EtherStreamUpdateS2C> CODEC = StreamCodec.composite(
            POSDIR_CODEC, EtherStreamUpdateS2C::posDir,
            ByteBufCodecs.collection(ArrayList::new, StreamEntry.STREAM_ENTRY_CODEC), EtherStreamUpdateS2C::entries,
            EtherStreamUpdateS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientVESHData.get().handleUpdate(this);
        });
    }
}
