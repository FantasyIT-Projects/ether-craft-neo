package studio.fantasyit.ether_craft.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public record PosDir(BlockPos pos, Direction dir) {
    public static final StreamCodec<RegistryFriendlyByteBuf, PosDir> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            PosDir::pos,
            Direction.STREAM_CODEC,
            PosDir::dir,
            PosDir::new
    );
}
