package studio.fantasyit.ether_craft.plating.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PlatingEffectFormula(double a1, double a2, double a3, double a4) {
    public static MapCodec<PlatingEffectFormula> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.fieldOf("a1").forGetter(PlatingEffectFormula::a1),
            Codec.DOUBLE.fieldOf("a2").forGetter(PlatingEffectFormula::a2),
            Codec.DOUBLE.fieldOf("a3").forGetter(PlatingEffectFormula::a3),
            Codec.DOUBLE.fieldOf("a4").forGetter(PlatingEffectFormula::a4)
    ).apply(instance, PlatingEffectFormula::new));
    public static Codec<PlatingEffectFormula> CODEC = MAP_CODEC.codec();
    public static StreamCodec<FriendlyByteBuf, PlatingEffectFormula> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            PlatingEffectFormula::a1,
            ByteBufCodecs.DOUBLE,
            PlatingEffectFormula::a2,
            ByteBufCodecs.DOUBLE,
            PlatingEffectFormula::a3,
            ByteBufCodecs.DOUBLE,
            PlatingEffectFormula::a4,
            PlatingEffectFormula::new
    );

    public double getEffect(long e) {
        if (e < a1) return 0;
        return (Math.clamp(e, a1, a2) - a1) / (a2 - a1) * (a4 - a3) + a3;
    }
}
