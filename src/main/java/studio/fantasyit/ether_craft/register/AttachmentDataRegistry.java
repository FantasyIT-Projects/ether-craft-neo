package studio.fantasyit.ether_craft.register;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.EtherStreamBlockStateReadCache;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHData;
import studio.fantasyit.ether_craft.plating.attachment.PlatingPlayerAttachment;
import studio.fantasyit.ether_craft.stream.vholder.VirtualEtherStreamHolderManager;

import java.util.Optional;
import java.util.function.Supplier;

public class AttachmentDataRegistry {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EtherCraft.MODID);
    public static final Supplier<AttachmentType<Long>> ETHER_CONTAINER = ATTACHMENT_TYPES.register(
            "ether_container", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("ether_container")).sync(ByteBufCodecs.LONG).build()
    );
    public static final Supplier<AttachmentType<Long>> ETHER_CONTAINER_MAX = ATTACHMENT_TYPES.register(
            "ether_container_max", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("ether_container_max")).build()
    );
    public static final Supplier<AttachmentType<VirtualEtherStreamHolderManager>> VESHM = ATTACHMENT_TYPES.register(
            "ether_stream_virtual_manager", () -> AttachmentType.builder(VirtualEtherStreamHolderManager::empty)
                    .serialize(VirtualEtherStreamHolderManager.CODEC.fieldOf("data"))
                    .build()
    );
    public static final Supplier<AttachmentType<ClientVESHData>> CLIENT_VESH_DATA = ATTACHMENT_TYPES.register(
            "client_vesh_data", () -> AttachmentType.builder(t -> ClientVESHData.get((Level) t)).build()
    );
    public static final Supplier<AttachmentType<Long>> CARRY_COOLDOWN = ATTACHMENT_TYPES.register(
            "carry_cooldown", () -> AttachmentType.builder(() -> -40L)
                    .serialize(Codec.LONG.fieldOf("carry_cooldown"))
                    .build()
    );
    public static final Supplier<AttachmentType<Optional<BlockPos>>> CARRY_COOLDOWN_SOURCE = ATTACHMENT_TYPES.register(
            "carry_cooldown_source", () -> AttachmentType.builder((Supplier<Optional<BlockPos>>) Optional::empty)
                    .serialize(BlockPos.CODEC.optionalFieldOf("carry_cooldown_source"))
                    .build()
    );

    public static final Supplier<AttachmentType<EtherStreamBlockStateReadCache>> ESBS_CACHE = ATTACHMENT_TYPES.register(
            "esbs_cache", () -> AttachmentType.builder(EtherStreamBlockStateReadCache::new)
                    .build()
    );

    public static final Supplier<AttachmentType<PlatingPlayerAttachment>> PLATING_PLAYER = ATTACHMENT_TYPES.register(
            "plating_player", () -> AttachmentType.builder(PlatingPlayerAttachment::new).build()
    );

    public static void register(IEventBus modbus) {
        ATTACHMENT_TYPES.register(modbus);
    }
}
