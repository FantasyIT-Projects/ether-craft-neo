package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;

public record SetBlockNameC2S(BlockPos pos, String name) implements CustomPacketPayload {
    public static final Type<@NotNull SetBlockNameC2S> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "set_block_name")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull SetBlockNameC2S> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SetBlockNameC2S::pos,
            ByteBufCodecs.STRING_UTF8,
            SetBlockNameC2S::name,
            SetBlockNameC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
