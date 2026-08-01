package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHDataGetter;
import studio.fantasyit.ether_craft.stream.idx.AutoIndexPosDir;

public record EtherStreamQuickCreateS2C(AutoIndexPosDir posDir) implements CustomPacketPayload {

    public static final Type<@NotNull EtherStreamQuickCreateS2C> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "es_quick")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull EtherStreamQuickCreateS2C> CODEC = StreamCodec.composite(
            AutoIndexPosDir.STREAM_CODEC, EtherStreamQuickCreateS2C::posDir,
            EtherStreamQuickCreateS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            PosDir resolved = posDir.resolve(ctx.player().level().getData(AttachmentDataRegistry.REVERSE_INDEX_MAPPING_MANAGER));
            if (resolved == null) return;
            ClientVESHDataGetter.get().handleQuickCreate(resolved, this);
        });
    }
}
