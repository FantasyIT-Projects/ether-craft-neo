package studio.fantasyit.ether_craft.plating.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public record CamouflageState(
        boolean isActive,
        int standStillTicks,
        BlockPos camouflagePos,
        float camouflageYaw,
        Vec3 lastPos
) {
    public static final CamouflageState INACTIVE = new CamouflageState(false, 0, BlockPos.ZERO, 0f, Vec3.ZERO);

    public static final StreamCodec<RegistryFriendlyByteBuf, CamouflageState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, CamouflageState::isActive,
            ByteBufCodecs.VAR_INT, CamouflageState::standStillTicks,
            BlockPos.STREAM_CODEC, CamouflageState::camouflagePos,
            ByteBufCodecs.FLOAT, CamouflageState::camouflageYaw,
            Vec3.STREAM_CODEC, CamouflageState::lastPos,
            CamouflageState::new
    );
}
