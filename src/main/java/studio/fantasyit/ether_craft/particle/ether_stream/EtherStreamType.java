package studio.fantasyit.ether_craft.particle.ether_stream;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class EtherStreamType extends ParticleType<EtherStreamData> {
    public EtherStreamType(boolean overrideLimiter) {
        super(overrideLimiter);
    }

    @Override
    public MapCodec<EtherStreamData> codec() {
        return EtherStreamData.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, EtherStreamData> streamCodec() {
        return EtherStreamData.STREAM_CODEC;
    }
}
