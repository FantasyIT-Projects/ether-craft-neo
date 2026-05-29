package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.stream.client.ClientVESHData;

import java.util.ArrayList;
import java.util.List;

public record EtherStreamSetDyingS2C(
        PosDir posDir,
        List<Integer> entries
) implements CustomPacketPayload {
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

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull EtherStreamSetDyingS2C> CODEC = StreamCodec.composite(
            POSDIR_CODEC, EtherStreamSetDyingS2C::posDir,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.INT), EtherStreamSetDyingS2C::entries,
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
