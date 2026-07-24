package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.EtherConsumer;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHDataGetter;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamEntryLike;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;
import studio.fantasyit.ether_craft.stream.data.SyncedEtherStreamDataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record EtherStreamBatchCreateS2C(
        PosDir posDir,
        List<StreamEntry> entries
) implements CustomPacketPayload {

    public record StreamEntry(
            int streamId,
            float startOffset,
            float startSpeed,
            int ether,
            int tickCount,
            EtherConsumer.State consumerState,
            List<IEtherStreamSyncedData> syncedData
    ) implements IEtherStreamEntryLike {
    }

    public static final Type<@NotNull EtherStreamBatchCreateS2C> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "es_batch")
    );
    private static final StreamCodec<RegistryFriendlyByteBuf, StreamEntry> STREAM_ENTRY_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StreamEntry::streamId,
            ByteBufCodecs.FLOAT, StreamEntry::startOffset,
            ByteBufCodecs.FLOAT, StreamEntry::startSpeed,
            ByteBufCodecs.VAR_INT, StreamEntry::ether,
            ByteBufCodecs.VAR_INT, StreamEntry::tickCount,
            EtherConsumer.State.STREAM_CODEC, StreamEntry::consumerState,
            ByteBufCodecs.collection(ArrayList::new, SyncedEtherStreamDataManager.STREAM_CODEC), StreamEntry::syncedData,
            StreamEntry::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull EtherStreamBatchCreateS2C> CODEC = StreamCodec.composite(
            PosDir.STREAM_CODEC, EtherStreamBatchCreateS2C::posDir,
            ByteBufCodecs.collection(ArrayList::new, STREAM_ENTRY_CODEC), EtherStreamBatchCreateS2C::entries,
            EtherStreamBatchCreateS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientVESHDataGetter.get().handleCreate(this);
        });
    }
}
