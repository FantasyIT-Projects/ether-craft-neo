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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.stream.client.ClientVESHData;

import java.util.Optional;

public record EtherStreamCreateS2C(
        PosDir posDir,
        int streamId,
        Vec3 startPos,
        Vec3 motion,
        int ether,
        int tickCount,
        @Nullable Component label,
        int labelColor
) implements CustomPacketPayload {
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

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull EtherStreamCreateS2C> CODEC = StreamCodec.composite(
            POSDIR_CODEC, EtherStreamCreateS2C::posDir,
            ByteBufCodecs.VAR_INT, EtherStreamCreateS2C::streamId,
            VEC3_CODEC, EtherStreamCreateS2C::startPos,
            VEC3_CODEC, EtherStreamCreateS2C::motion,
            ByteBufCodecs.VAR_INT, EtherStreamCreateS2C::ether,
            ByteBufCodecs.VAR_INT, EtherStreamCreateS2C::tickCount,
            NULLABLE_COMPONENT_CODEC, EtherStreamCreateS2C::label,
            ByteBufCodecs.INT, EtherStreamCreateS2C::labelColor,
            EtherStreamCreateS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientVESHData.get().handleCreate(this);
        });
    }
}
