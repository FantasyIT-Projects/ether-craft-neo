package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.stream.EtherConsumer;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHDataGetter;
import studio.fantasyit.ether_craft.stream.idx.AutoIndexPosDir;

import java.util.List;
import java.util.Optional;

public record EtherStreamUpdateS2C(
        AutoIndexPosDir posDir,
        List<StreamEntry> entries
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull EtherStreamUpdateS2C> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "es_update")
    );

    public record StreamEntry(int streamId, int ether, Optional<EtherConsumer.State> consumerState) {
        public static final StreamCodec<RegistryFriendlyByteBuf, StreamEntry> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, StreamEntry::streamId,
                ByteBufCodecs.VAR_INT, StreamEntry::ether,
                ByteBufCodecs.optional(EtherConsumer.State.STREAM_CODEC), StreamEntry::consumerState,
                StreamEntry::new
        );
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull EtherStreamUpdateS2C> CODEC = StreamCodec.composite(
            AutoIndexPosDir.STREAM_CODEC,
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
        ctx.enqueueWork(() -> {
            PosDir resolved = posDir.resolve(ctx.player().level().getData(AttachmentDataRegistry.REVERSE_INDEX_MAPPING_MANAGER));
            if (resolved == null) return;
            ClientVESHDataGetter.get().handleUpdate(resolved, this);
        });
    }
}
