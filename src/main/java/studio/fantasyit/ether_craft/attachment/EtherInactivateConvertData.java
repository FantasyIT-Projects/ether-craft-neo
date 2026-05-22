package studio.fantasyit.ether_craft.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import studio.fantasyit.ether_craft.EtherCraft;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class EtherInactivateConvertData extends SavedData {
    public Map<UUID, Integer> convertCounter;
    public Map<UUID, Integer> lastTick;

    public static final SavedDataType<EtherInactivateConvertData> ID = new SavedDataType<>(
            EtherCraft.id("ether_inactivate_convert_data"),
            EtherInactivateConvertData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).fieldOf("convertCounter").forGetter(sd -> sd.convertCounter),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).fieldOf("lastTick").forGetter(sd -> sd.lastTick)
            ).apply(instance, EtherInactivateConvertData::new))
    );

    public EtherInactivateConvertData() {
        this.convertCounter = new HashMap<>();
        this.lastTick = new HashMap<>();
    }

    public EtherInactivateConvertData(Map<UUID, Integer> convertCounter, Map<UUID, Integer> lastTick) {
        this.convertCounter = new HashMap<>(convertCounter);
        this.lastTick = new HashMap<>(lastTick);
    }

    public void tick() {
        HashSet<UUID> ks = new HashSet<>(convertCounter.keySet());
        for (UUID uuid : ks) {
            lastTick.put(uuid, lastTick.getOrDefault(uuid, 0) + 1);
            if (lastTick.getOrDefault(uuid, 0) > 20) {
                convertCounter.remove(uuid);
                lastTick.remove(uuid);
                setDirty();
            }
        }
    }

    public int entityTick(UUID uuid, int currentTick) {
        convertCounter.put(uuid, convertCounter.getOrDefault(uuid, 0) + 1);
        lastTick.put(uuid, 0);
        setDirty();
        return convertCounter.get(uuid);
    }

    public void reset(UUID uuid) {
        convertCounter.remove(uuid);
        lastTick.remove(uuid);
        setDirty();
    }
}
