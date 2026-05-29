package studio.fantasyit.ether_craft.stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CapabilityFactoryManager {
    private static final Map<Identifier, Entry<?>> ENTRIES = new HashMap<>();

    public static <T extends IStreamCapability> void register(Identifier id, Supplier<T> factory, Codec<T> codec) {
        ENTRIES.put(id, new Entry<>(factory, codec));
    }

    public static IStreamCapability create(Identifier id) {
        Entry<?> entry = ENTRIES.get(id);
        if (entry == null) return null;
        return entry.factory.get();
    }

    @SuppressWarnings("unchecked")
    public static MapCodec<IStreamCapability> getMapCodec(String id) {
        Entry<?> entry = ENTRIES.get(Identifier.parse(id));
        if (entry == null) return null;
        return ((Codec<IStreamCapability>) entry.codec).fieldOf("data");
    }

    public static final Codec<IStreamCapability> CODEC = Codec.STRING.dispatch(
            cap -> cap.getId().toString(),
            CapabilityFactoryManager::getMapCodec
    );

    private record Entry<T extends IStreamCapability>(Supplier<T> factory, Codec<T> codec) {}
}
