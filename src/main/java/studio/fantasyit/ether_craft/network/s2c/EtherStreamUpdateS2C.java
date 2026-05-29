package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.EtherConsumer;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.client.ClientVESHDataGetter;

import java.util.List;

public record EtherStreamUpdateS2C(
        PosDir posDir,
        List<StreamEntry> entries
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull EtherStreamUpdateS2C> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "ether_stream_update")
    );

    public record StreamEntry(int streamId, int ether, EtherConsumer.State consumerState) {
        public static final StreamCodec<RegistryFriendlyByteBuf, StreamEntry> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, StreamEntry::streamId,
                ByteBufCodecs.INT, StreamEntry::ether,
                EtherConsumer.State.CODEC, StreamEntry::consumerState,
                StreamEntry::new
        );
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull EtherStreamUpdateS2C> CODEC = StreamCodec.composite(
            PosDir.STREAM_CODEC,
            EtherStreamUpdateS2C::posDir,
            StreamEntry.CODEC.apply(ByteBufCodecs.list()),
            EtherStreamUpdateS2C::entries,
            EtherStreamUpdateS2C::new
    );

    @Override
    public @NotNull Type<@NotNull EtherStreamUpdateS2C> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientVESHDataGetter.get().handleUpdate(this));
    }
}
