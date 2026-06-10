package studio.fantasyit.ether_craft.plating.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record ProgressingPlatingData(Identifier id, PlatingEffectFormula formula) {
    public static final Codec<ProgressingPlatingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(ProgressingPlatingData::id),
            PlatingEffectFormula.CODEC.fieldOf("eff").forGetter(ProgressingPlatingData::formula)
    ).apply(instance, ProgressingPlatingData::new));
    public static final StreamCodec<FriendlyByteBuf, ProgressingPlatingData> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC,
            ProgressingPlatingData::id,
            PlatingEffectFormula.STREAM_CODEC,
            ProgressingPlatingData::formula,
            ProgressingPlatingData::new
    );
}
