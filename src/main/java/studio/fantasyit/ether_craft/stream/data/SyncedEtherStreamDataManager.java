package studio.fantasyit.ether_craft.stream.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public class SyncedEtherStreamDataManager {
    public static Map<Identifier, IEtherStreamSyncedData.Builder> syncedDataBuilders = new HashMap<>();
    public static Map<Identifier, MapCodec<IEtherStreamSyncedData>> syncedDataCodecs = new HashMap<>();

    public static StreamCodec<RegistryFriendlyByteBuf, IEtherStreamSyncedData> STREAM_CODEC = StreamCodec.of((t, a) -> {
        t.writeIdentifier(a.getId());
        a.toBuffer(t);
    }, t -> {
        Identifier id = t.readIdentifier();
        return syncedDataBuilders.get(id).build(t);
    });

    public static final Codec<IEtherStreamSyncedData> CODEC = Identifier.CODEC.dispatch(
            "id", IEtherStreamSyncedData::getId, id -> syncedDataCodecs.get(id)
    );

    @SuppressWarnings("unchecked")
    public static <T extends IEtherStreamSyncedData> void register(Identifier id, IEtherStreamSyncedData.Builder builder, MapCodec<T> codec) {
        syncedDataBuilders.put(id, builder);
        syncedDataCodecs.put(id, (MapCodec<IEtherStreamSyncedData>) codec);
    }

    public static void collect() {
        register(EtherStreamLabelData.ID, EtherStreamLabelData::fromBuffer, EtherStreamLabelData.CODEC);
        register(EtherStreamCarryingEntityData.ID, EtherStreamCarryingEntityData::fromBuffer, EtherStreamCarryingEntityData.CODEC);
    }
}
