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

public record EtherStreamInitialCreateS2C(
        PosDir posDir,
        int streamId,
        float startOffset,
        float startSpeed,
        int ether,
        EtherConsumer.State consumerState,
        List<IEtherStreamSyncedData> syncedData
) implements CustomPacketPayload, IEtherStreamEntryLike {

    public static final Type<@NotNull EtherStreamInitialCreateS2C> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "es_init")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull EtherStreamInitialCreateS2C> CODEC = StreamCodec.composite(
            PosDir.STREAM_CODEC, EtherStreamInitialCreateS2C::posDir,
            ByteBufCodecs.VAR_INT, EtherStreamInitialCreateS2C::streamId,
            ByteBufCodecs.FLOAT, EtherStreamInitialCreateS2C::startOffset,
            ByteBufCodecs.FLOAT, EtherStreamInitialCreateS2C::startSpeed,
            ByteBufCodecs.VAR_INT, EtherStreamInitialCreateS2C::ether,
            EtherConsumer.State.STREAM_CODEC, EtherStreamInitialCreateS2C::consumerState,
            ByteBufCodecs.collection(ArrayList::new, SyncedEtherStreamDataManager.STREAM_CODEC), EtherStreamInitialCreateS2C::syncedData,
            EtherStreamInitialCreateS2C::new
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

    @Override
    public int tickCount() {
        return 1;
    }
}
