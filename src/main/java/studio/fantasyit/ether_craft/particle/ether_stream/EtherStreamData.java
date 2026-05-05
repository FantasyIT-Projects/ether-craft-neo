package studio.fantasyit.ether_craft.particle.ether_stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.register.ParticleRegistry;

public class EtherStreamData implements ParticleOptions {

    public static final MapCodec<EtherStreamData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("color").forGetter(EtherStreamData::getColor),
            Codec.FLOAT.fieldOf("size").forGetter(EtherStreamData::getSize)
    ).apply(instance, EtherStreamData::new));
    public static final StreamCodec<? super FriendlyByteBuf, EtherStreamData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    EtherStreamData::getColor,
                    ByteBufCodecs.FLOAT,
                    EtherStreamData::getSize,
                    EtherStreamData::new
            );

    public int color;
    public float size;

    public EtherStreamData(int color, float size) {
        this.color = color;
        this.size = size;
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return ParticleRegistry.ETHER_STREAM_PARTICLE.get();
    }

    public int getColor() {
        return color;
    }

    public float getSize() {
        return size;
    }
}
