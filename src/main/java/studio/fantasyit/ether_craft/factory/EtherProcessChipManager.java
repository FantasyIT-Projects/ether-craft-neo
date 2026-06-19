package studio.fantasyit.ether_craft.factory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class EtherProcessChipManager {
    private static final Map<Identifier, IProcessChipBehavior> BEHAVIORS = new HashMap<>();

    public static void registerBehavior(Identifier id, IProcessChipBehavior behavior) {
        BEHAVIORS.put(id, behavior);
    }

    public static @Nullable IProcessChipBehavior getBehavior(Identifier id) {
        return BEHAVIORS.get(id);
    }

    public static void update(Map<Identifier, ProcessChipRecord> identifierProcessChipRecordMap) {
        chipInfo = identifierProcessChipRecordMap;
    }

    public static Map<Identifier, ProcessChipRecord> chipInfo = new HashMap<>();

    public static void foreach(BiConsumer<Identifier, ProcessChipRecord> consumer) {
        chipInfo.forEach(consumer);
    }

    public record ProcessChipEffectConfig(boolean separate, boolean effectSelf, boolean effectSide) {
        public static final ProcessChipEffectConfig DEFAULT = new ProcessChipEffectConfig(true, false, true);
        public static final Codec<ProcessChipEffectConfig> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.BOOL.fieldOf("separate").orElse(true).forGetter(ProcessChipEffectConfig::separate),
                Codec.BOOL.fieldOf("effectSelf").orElse(true).forGetter(ProcessChipEffectConfig::effectSelf),
                Codec.BOOL.fieldOf("effectSide").orElse(true).forGetter(ProcessChipEffectConfig::effectSide)
        ).apply(inst, ProcessChipEffectConfig::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ProcessChipEffectConfig> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL,
                ProcessChipEffectConfig::separate,
                ByteBufCodecs.BOOL,
                ProcessChipEffectConfig::effectSelf,
                ByteBufCodecs.BOOL,
                ProcessChipEffectConfig::effectSide,
                ProcessChipEffectConfig::new
        );
    }

    public record ProcessChipRecord(long maxEther, int etherDecay, long etherRequire, long etherConsume,
                                    ProcessChipEffectConfig effect, int maxDurability,
                                    Optional<Identifier> behavior) {
        public static final Codec<ProcessChipRecord> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.LONG.fieldOf("maxEther").forGetter(ProcessChipRecord::maxEther),
                Codec.INT.fieldOf("etherDecay").forGetter(ProcessChipRecord::etherDecay),
                Codec.LONG.fieldOf("etherRequire").forGetter(ProcessChipRecord::etherRequire),
                Codec.LONG.fieldOf("etherConsume").forGetter(ProcessChipRecord::etherConsume),
                ProcessChipEffectConfig.CODEC.optionalFieldOf("effect", ProcessChipEffectConfig.DEFAULT).forGetter(ProcessChipRecord::effect),
                Codec.INT.optionalFieldOf("maxDurability", 0).forGetter(ProcessChipRecord::maxDurability),
                Identifier.CODEC.optionalFieldOf("behavior").forGetter(ProcessChipRecord::behavior)
        ).apply(inst, ProcessChipRecord::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ProcessChipRecord> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_LONG, EtherProcessChipManager.ProcessChipRecord::maxEther,
                        ByteBufCodecs.VAR_INT, EtherProcessChipManager.ProcessChipRecord::etherDecay,
                        ByteBufCodecs.VAR_LONG, EtherProcessChipManager.ProcessChipRecord::etherRequire,
                        ByteBufCodecs.VAR_LONG, EtherProcessChipManager.ProcessChipRecord::etherConsume,
                        ProcessChipEffectConfig.STREAM_CODEC, ProcessChipRecord::effect,
                        ByteBufCodecs.VAR_INT, EtherProcessChipManager.ProcessChipRecord::maxDurability,
                        ByteBufCodecs.optional(Identifier.STREAM_CODEC), EtherProcessChipManager.ProcessChipRecord::behavior,
                        EtherProcessChipManager.ProcessChipRecord::new
                );

    }

    public static ProcessChipRecord get(Identifier identifier) {
        return chipInfo.get(identifier);
    }

    public static @Nullable ProcessChipRecord get(ItemStack item) {
        if (!item.is(ItemRegistry.PROCESS_CHIP_ITEM)) return null;
        @Nullable Identifier i = item.get(DataComponentRegistry.CHIP_ID);
        if (i == null) return null;
        return chipInfo.get(i);
    }
}
