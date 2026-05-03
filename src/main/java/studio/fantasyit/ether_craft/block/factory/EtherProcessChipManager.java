package studio.fantasyit.ether_craft.block.factory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

}
