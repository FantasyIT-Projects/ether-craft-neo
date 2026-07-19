package studio.fantasyit.ether_craft.attachment;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record MuteRange(BlockPos pos, int rx, int ry, int rz) {
    public static final StreamCodec<FriendlyByteBuf, MuteRange> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MuteRange::pos,
            ByteBufCodecs.INT, MuteRange::rx,
            ByteBufCodecs.INT, MuteRange::ry,
            ByteBufCodecs.INT, MuteRange::rz,
            MuteRange::new
    );
}