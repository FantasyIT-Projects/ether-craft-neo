package studio.fantasyit.ether_craft.factory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class EtherProcessChipManager {
    public static void update(Map<Identifier, ProcessChipRecord> identifierProcessChipRecordMap) {
        chipInfo = identifierProcessChipRecordMap;
    }

    public static Map<Identifier, ProcessChipRecord> chipInfo = new HashMap<>();

    public static void foreach(BiConsumer<Identifier, ProcessChipRecord>  consumer) {
        chipInfo.forEach(consumer);
    }

    public record ProcessChipRecord( long maxEther, int etherDecay, long etherRequire, long etherConsume){
        public static final Codec<ProcessChipRecord> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.LONG.fieldOf("maxEther").forGetter(ProcessChipRecord::maxEther),
                Codec.INT.fieldOf("etherDecay").forGetter(ProcessChipRecord::etherDecay),
                Codec.LONG.fieldOf("etherRequire").forGetter(ProcessChipRecord::etherRequire),
                Codec.LONG.fieldOf("etherConsume").forGetter(ProcessChipRecord::etherConsume)
        ).apply(inst, ProcessChipRecord::new));
    }

    public static ProcessChipRecord get(Identifier identifier) {
        return chipInfo.get(identifier);
    }

    public static @Nullable ProcessChipRecord get(ItemStack item) {
        if(!item.is(ItemRegistry.PROCESS_CHIP_ITEM)) return null;
        @Nullable Identifier i = item.get(DataComponentRegistry.CHIP_ID);
        if(i == null) return null;
        return chipInfo.get(i);
    }

}
