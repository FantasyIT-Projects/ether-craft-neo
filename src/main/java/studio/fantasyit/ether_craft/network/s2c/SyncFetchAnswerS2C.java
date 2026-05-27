package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.menu.grid.answer.AnswerFetchMenu;
import studio.fantasyit.ether_craft.recipe.grid.EtherProcessFactoryGrid;

public record SyncFetchAnswerS2C(EtherProcessFactoryGrid grid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull SyncFetchAnswerS2C> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(
                    EtherCraft.MODID, "sync_fetch_answer"
            )
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull SyncFetchAnswerS2C> CODEC = StreamCodec.composite(
            EtherProcessFactoryGrid.STREAM_CODEC,
            SyncFetchAnswerS2C::grid,
            SyncFetchAnswerS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            if (iPayloadContext.player().containerMenu instanceof AnswerFetchMenu menu) {
                menu.selectedGrid = grid;
            }
        });
    }
}
