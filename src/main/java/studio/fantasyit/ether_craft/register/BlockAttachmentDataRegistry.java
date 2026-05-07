package studio.fantasyit.ether_craft.register;

import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import studio.fantasyit.ether_craft.EtherCraft;

import java.util.function.Supplier;

public class BlockAttachmentDataRegistry {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EtherCraft.MODID);
    public static final Supplier<AttachmentType<Long>> ETHER_CONTAINER = ATTACHMENT_TYPES.register(
            "ether_container", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("ether_container")).build()
    );public static final Supplier<AttachmentType<Long>> ETHER_CONTAINER_MAX = ATTACHMENT_TYPES.register(
            "ether_container_max", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("ether_container_max")).build()
    );

    public static void register(IEventBus modbus) {
        ATTACHMENT_TYPES.register(modbus);
    }
}
