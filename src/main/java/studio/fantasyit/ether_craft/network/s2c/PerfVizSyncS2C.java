package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.client.debug.PerfVizData;
import studio.fantasyit.ether_craft.perf.PerfTickData;

import java.util.List;

public record PerfVizSyncS2C(
        List<PerfTickData.VeshSnapshot> veshEntries,
        List<PerfTickData.BlockSnapshot> blockEntries
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull PerfVizSyncS2C> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "perf_viz_sync")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull PerfVizSyncS2C> CODEC = StreamCodec.composite(
            PerfTickData.VeshSnapshot.STREAM_CODEC.apply(ByteBufCodecs.list()),
            PerfVizSyncS2C::veshEntries,
            PerfTickData.BlockSnapshot.STREAM_CODEC.apply(ByteBufCodecs.list()),
            PerfVizSyncS2C::blockEntries,
            PerfVizSyncS2C::new
    );

    @Override
    public @NotNull Type<@NotNull PerfVizSyncS2C> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> PerfVizData.handleSync(this));
    }
}
