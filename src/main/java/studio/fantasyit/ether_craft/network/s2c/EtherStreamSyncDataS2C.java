package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHData;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;
import studio.fantasyit.ether_craft.stream.data.SyncedEtherStreamDataManager;
import studio.fantasyit.ether_craft.stream.idx.AutoIndexPosDir;

import java.util.ArrayList;
import java.util.List;

public record EtherStreamSyncDataS2C(AutoIndexPosDir posDir, int streamId,
                                     List<IEtherStreamSyncedData> data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull EtherStreamSyncDataS2C> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "es_sync")
    );


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull EtherStreamSyncDataS2C> CODEC = StreamCodec.composite(
            AutoIndexPosDir.STREAM_CODEC,
            EtherStreamSyncDataS2C::posDir,
            ByteBufCodecs.VAR_INT, EtherStreamSyncDataS2C::streamId,
            ByteBufCodecs.collection(ArrayList::new, SyncedEtherStreamDataManager.STREAM_CODEC), EtherStreamSyncDataS2C::data,
            EtherStreamSyncDataS2C::new
    );

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            PosDir resolved = posDir.resolve(ctx.player().level().getData(AttachmentDataRegistry.REVERSE_INDEX_MAPPING_MANAGER));
            if (resolved == null) return;
            ClientVESHData.getWithCurrentLevel(ctx.player().level()).handleSync(resolved, this);
        });
    }
}
