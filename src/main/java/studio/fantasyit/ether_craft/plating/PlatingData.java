package studio.fantasyit.ether_craft.plating;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record PlatingData(Identifier id, double effect, @Nullable Long coolDownUntil) {
    public PlatingData(Identifier id, double effect) {
        this(id, effect, null);
    }

    public static final Codec<PlatingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(PlatingData::id),
            Codec.DOUBLE.fieldOf("effect").forGetter(PlatingData::effect),
            Codec.LONG.optionalFieldOf("coolDownUntil").forGetter(d -> Optional.ofNullable(d.coolDownUntil))
    ).apply(instance, (id, effect, cd) -> new PlatingData(id, effect, cd.orElse(null))));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlatingData> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, PlatingData::id,
            ByteBufCodecs.DOUBLE, PlatingData::effect,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_LONG), d -> Optional.ofNullable(d.coolDownUntil),
            (id, effect, cd) -> new PlatingData(id, effect, cd.orElse(null))
    );

    public PlatingData copyWithCoolDown(ServerLevel level, long cdTicks) {
        return new PlatingData(id, effect, level.getGameTime() + cdTicks);
    }

    public boolean isCd(ServerLevel level) {
        return coolDownUntil != null && level.getGameTime() < coolDownUntil;
    }
}
