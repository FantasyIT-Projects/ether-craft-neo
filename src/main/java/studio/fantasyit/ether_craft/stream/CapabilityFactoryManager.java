package studio.fantasyit.ether_craft.stream;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.stream.cap.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CapabilityFactoryManager {
    private static final Map<Identifier, Entry<?>> ENTRIES = new HashMap<>();

    public static void init() {
        register(EtherStreamBreakBlockCapability.ID, EtherStreamBreakBlockCapability::new, EtherStreamBreakBlockCapability.CODEC);
        register(EtherStreamGrowthAcceleratorCapability.ID, () -> new EtherStreamGrowthAcceleratorCapability(false), EtherStreamGrowthAcceleratorCapability.CODEC);
        register(EtherStreamGrowthAcceleratorCapability.ID_ALL, () -> new EtherStreamGrowthAcceleratorCapability(true), EtherStreamGrowthAcceleratorCapability.CODEC_ALLOW_ALL);
        register(EtherStreamStorageCapability.ID, () -> new EtherStreamStorageCapability(1), EtherStreamStorageCapability.CODEC);
        register(EtherStreamDamageCapability.ID, EtherStreamDamageCapability::new, EtherStreamDamageCapability.CODEC);
        register(EtherStreamCarryEntityCapability.ID, () -> new EtherStreamCarryEntityCapability(BlockPos.ZERO), EtherStreamCarryEntityCapability.CODEC);
        register(EtherStreamCarryEntityCapability.ID_PLAYER, () -> new EtherStreamCarryEntityCapability(BlockPos.ZERO, true), EtherStreamCarryEntityCapability.CODEC_PLAYER);
        register(EtherStreamCostReducerCapability.ID, EtherStreamCostReducerCapability::new, EtherStreamCostReducerCapability.CODEC);
        register(EtherStreamPlatingCapability.ID, EtherStreamPlatingCapability::new, EtherStreamPlatingCapability.CODEC);
        register(EtherStreamBounceBackCapability.ID, EtherStreamBounceBackCapability::new, EtherStreamBounceBackCapability.CODEC);
        register(EtherStreamItemDisplayCapability.ID, EtherStreamItemDisplayCapability::new, EtherStreamItemDisplayCapability.CODEC);
        register(EtherStreamTransformGlassCapability.ID, EtherStreamTransformGlassCapability::new, Codec.EMPTY.xmap(t -> new EtherStreamTransformGlassCapability(), t -> Unit.INSTANCE).codec());
    }

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

    private record Entry<T extends IStreamCapability>(Supplier<T> factory, Codec<T> codec) {
    }
}
