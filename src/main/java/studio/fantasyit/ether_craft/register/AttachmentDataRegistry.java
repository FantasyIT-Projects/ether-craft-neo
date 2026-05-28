package studio.fantasyit.ether_craft.register;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache;

import java.util.function.Supplier;

public class AttachmentDataRegistry {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EtherCraft.MODID);
    public static final Supplier<AttachmentType<Long>> ETHER_CONTAINER = ATTACHMENT_TYPES.register(
            "ether_container", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("ether_container")).sync(ByteBufCodecs.LONG).build()
    );
    public static final Supplier<AttachmentType<Long>> ETHER_CONTAINER_MAX = ATTACHMENT_TYPES.register(
            "ether_container_max", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("ether_container_max")).build()
    );
    public static final Supplier<AttachmentType<ChainedEmitterEntityHitCache>> CHAINED_EMITTER_ENTITY_HIT_CACHE = ATTACHMENT_TYPES.register(
            "ether_inactivate_convert_data", () -> AttachmentType.builder(ChainedEmitterEntityHitCache::new).build()
    );

    public static void register(IEventBus modbus) {
        ATTACHMENT_TYPES.register(modbus);
    }
}
