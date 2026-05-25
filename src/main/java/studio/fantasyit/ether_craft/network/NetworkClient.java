package studio.fantasyit.ether_craft.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.network.c2s.*;

import java.util.function.BiConsumer;

public class NetworkClient {
    public static void clientMsg(PayloadRegistrar event) {
        event.playToServer(
                TriggerSwitchTabC2S.TYPE,
                TriggerSwitchTabC2S.CODEC,
                wrapWithPlayer(TriggerSwitchTabC2SHandler::handle)
        );
        event.playToServer(
                SetFilterSlotC2S.TYPE,
                SetFilterSlotC2S.CODEC,
                wrapWithPlayer(SetFilterSlotC2SHandler::handle)
        );
        event.playToServer(
                SyncScreenDataC2S.TYPE,
                SyncScreenDataC2S.CODEC,
                wrapWithPlayer(SyncScreenDataC2SHandler::handle)
        );
        event.playToServer(
                SyncFilterActiveC2S.TYPE,
                SyncFilterActiveC2S.CODEC,
                wrapWithPlayer(SyncFilterActiveC2SHandler::handle)
        );
        event.playToServer(
                SetBlockNameC2S.TYPE,
                SetBlockNameC2S.CODEC,
                wrapWithPlayer(SetBlockNameC2SHandler::handle)
        );
        event.playToServer(
                FactoryMenuSwitchItemC2S.TYPE,
                FactoryMenuSwitchItemC2S.CODEC,
                wrapWithPlayer(FactoryMenuSwitchItemC2SHandler::handle)
        );
    }

    private static <T extends CustomPacketPayload> IPayloadHandler<@NotNull T> wrapWithPlayer(BiConsumer<T, Player> t) {
        return (d, c) -> {
            c.enqueueWork(() -> t.accept(d, c.player()));
        };
    }
}
