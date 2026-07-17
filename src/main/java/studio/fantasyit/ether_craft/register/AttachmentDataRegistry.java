package studio.fantasyit.ether_craft.register;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.CamouflageState;
import studio.fantasyit.ether_craft.plating.data.TrackingData;
import studio.fantasyit.ether_craft.plating.trigger.data.TriggerOnNotExistRecord;
import studio.fantasyit.ether_craft.stream.vholder.VirtualEtherStreamHolderManager;

import java.util.HashSet;
import java.util.Optional;
import java.util.function.Supplier;

public class AttachmentDataRegistry {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EtherCraft.MODID);
    public static final Supplier<AttachmentType<Long>> ETHER_CONTAINER = ATTACHMENT_TYPES.register(
            "ether_container", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("ether_container")).build()
    );
    public static final Supplier<AttachmentType<Long>> ETHER_CONTAINER_MAX = ATTACHMENT_TYPES.register(
            "ether_container_max", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("ether_container_max")).build()
    );
    public static final Supplier<AttachmentType<VirtualEtherStreamHolderManager>> VESHM = ATTACHMENT_TYPES.register(
            "ether_stream_virtual_manager", () -> AttachmentType.builder(VirtualEtherStreamHolderManager::empty)
                    .serialize(VirtualEtherStreamHolderManager.CODEC.fieldOf("data"))
                    .build()
    );

    public static final Supplier<AttachmentType<Long>> PICK_UP_BY_STREAM_COOLDOWN = ATTACHMENT_TYPES.register(
            "pickup_by_stream_cooldown", () -> AttachmentType.builder(() -> 0L)
                    .serialize(Codec.LONG.fieldOf("pickup_by_stream_cooldown"))
                    .build()
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

    public static final Supplier<AttachmentType<TriggerOnNotExistRecord>> TRIGGER_ON_NOT_EXIST_RECORD = ATTACHMENT_TYPES.register(
            "trigger_on_not_exist_plating_tracker", () -> AttachmentType.builder(() -> new TriggerOnNotExistRecord(new HashSet<>()))
                    .sync(TriggerOnNotExistRecord.STREAM_CODEC)
                    .serialize(TriggerOnNotExistRecord.CODEC.fieldOf("data"))
                    .build()
    );

    public static final Supplier<AttachmentType<Boolean>> TAKEN_BY_ETHER_STREAM = ATTACHMENT_TYPES.register(
            "taken_by_ether_stream", () -> AttachmentType.builder(() -> false)
                    .sync(ByteBufCodecs.BOOL)
                    .build()
    );

    public static final Supplier<AttachmentType<Integer>> CD_TO_TAKE_BY_ETHER_STREAM = ATTACHMENT_TYPES.register(
            "cooldown_untile_taken_by_ether_stream", () -> AttachmentType.builder(() -> 0)
                    .sync(ByteBufCodecs.INT)
                    .build()
    );
    public static final Supplier<AttachmentType<CamouflageState>> CAMOUFLAGE_STATE = ATTACHMENT_TYPES.register(
            "camouflage_state", () -> AttachmentType.builder(() -> CamouflageState.INACTIVE)
                    .sync(CamouflageState.STREAM_CODEC)
                    .build()
    );

    public static final Supplier<AttachmentType<TrackingData>> ARROW_TRACKING = ATTACHMENT_TYPES.register(
            "arrow_tracking", () -> AttachmentType.builder(() -> new TrackingData(0.0, 0.0)).sync(TrackingData.STREAM_CODEC).build()
    );

    public static void register(IEventBus modbus) {
        ATTACHMENT_TYPES.register(modbus);
    }
}
