package studio.fantasyit.ether_craft.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

import java.util.ArrayList;
import java.util.Map;

public class SerializeUtil {
    public record PDMap(InstalledPlugin plugin, Direction direction) {
        public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull PDMap> CODEC = StreamCodec.composite(
                InstalledPlugin.STREAM_CODEC,
                PDMap::plugin,
                Direction.STREAM_CODEC,
                PDMap::direction,
                PDMap::new
        );

        public static ArrayList<PDMap> fromMap(Map<Direction, InstalledPlugin> map) {
            return new ArrayList<>(map.entrySet().stream().map(entry -> new PDMap(entry.getValue(), entry.getKey())).toList());
        }

        public static Map<Direction, InstalledPlugin> toMap(ArrayList<PDMap> list) {
            return list.stream().collect(java.util.stream.Collectors.toMap(PDMap::direction, PDMap::plugin));
        }
    }

    public record PIMap(Identifier plugin, Integer value) {
        public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull PIMap> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC,
                PIMap::plugin,
                ByteBufCodecs.INT,
                PIMap::value,
                PIMap::new
        );
        public static final Codec<Map<Identifier, Integer>> CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.INT);

        public static ArrayList<PIMap> fromMap(Map<Identifier, Integer> map) {
            return new ArrayList<>(map.entrySet().stream().map(entry -> new PIMap(entry.getKey(), entry.getValue())).toList());
        }

        public static Map<Identifier, Integer> toMap(ArrayList<PIMap> list) {
            return list.stream().collect(java.util.stream.Collectors.toMap(PIMap::plugin, PIMap::value));
        }
    }
}
