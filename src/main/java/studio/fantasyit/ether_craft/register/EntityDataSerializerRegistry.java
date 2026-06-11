package studio.fantasyit.ether_craft.register;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;
import studio.fantasyit.ether_craft.stream.data.SyncedEtherStreamDataManager;

import java.util.ArrayList;
import java.util.List;

public class EntityDataSerializerRegistry {
    public static final DeferredRegister<EntityDataSerializer<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, EtherCraft.MODID);

    public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<List<IEtherStreamSyncedData>>> SYNCED_DATA_LIST =
            REGISTER.register("synced_data_list", () -> new EntityDataSerializer<>() {
                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, List<IEtherStreamSyncedData>> codec() {
                    return ByteBufCodecs.collection(ArrayList::new, SyncedEtherStreamDataManager.STREAM_CODEC);
                }

                @Override
                public List<IEtherStreamSyncedData> copy(List<IEtherStreamSyncedData> value) {
                    return new ArrayList<>(value);
                }
            });

    public static void register(IEventBus eventBus) {
        REGISTER.register(eventBus);
    }
}
