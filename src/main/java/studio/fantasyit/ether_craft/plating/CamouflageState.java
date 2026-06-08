package studio.fantasyit.ether_craft.plating;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CamouflageState(
        boolean isActive,
        int standStillTicks,
        BlockPos camouflagePos,
        float camouflageYaw,
        long lastPosHash
) {
    public static final CamouflageState INACTIVE = new CamouflageState(false, 0, BlockPos.ZERO, 0f, 0L);

    public static final StreamCodec<RegistryFriendlyByteBuf, CamouflageState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, CamouflageState::isActive,
            ByteBufCodecs.VAR_INT, CamouflageState::standStillTicks,
            BlockPos.STREAM_CODEC, CamouflageState::camouflagePos,
            ByteBufCodecs.FLOAT, CamouflageState::camouflageYaw,
            ByteBufCodecs.VAR_LONG, CamouflageState::lastPosHash,
            CamouflageState::new
    );
}
