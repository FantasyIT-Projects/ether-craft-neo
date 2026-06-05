package studio.fantasyit.ether_craft.plating;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record PlatingData(Identifier id, double effect) {
    public static final Codec<PlatingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(PlatingData::id),
            Codec.DOUBLE.fieldOf("effect").forGetter(PlatingData::effect)
    ).apply(instance, PlatingData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlatingData> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, PlatingData::id,
            ByteBufCodecs.DOUBLE, PlatingData::effect,
            PlatingData::new
    );
}
