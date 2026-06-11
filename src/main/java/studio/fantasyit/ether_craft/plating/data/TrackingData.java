package studio.fantasyit.ether_craft.plating.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record TrackingData(double range, double strength) {
    public static final StreamCodec<RegistryFriendlyByteBuf, TrackingData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            TrackingData::range,
            ByteBufCodecs.DOUBLE,
            TrackingData::strength,
            TrackingData::new
    );
}
