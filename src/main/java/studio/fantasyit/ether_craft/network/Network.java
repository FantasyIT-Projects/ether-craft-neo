package studio.fantasyit.ether_craft.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.network.c2s.*;
import studio.fantasyit.ether_craft.network.s2c.*;

import java.util.function.BiConsumer;

public class Network {
    private static void commonMsg(PayloadRegistrar event) {
        event.playToClient(
                SyncBlockEtherValueS2C.TYPE,
                SyncBlockEtherValueS2C.CODEC,
                SyncBlockEtherValueS2C::handle
        );
        event.playToClient(
                SyncEtherAdaptNodeExtraS2C.TYPE,
                SyncEtherAdaptNodeExtraS2C.CODEC,
                SyncEtherAdaptNodeExtraS2C::handle
        );
        event.playToClient(
                SyncBlockNameS2C.TYPE,
                SyncBlockNameS2C.CODEC,
                SyncBlockNameS2C::handle
        );
        event.playToClient(
                SyncExtraRecipesS2C.TYPE,
                SyncExtraRecipesS2C.CODEC,
                SyncExtraRecipesS2C::handle
        );
        event.playToClient(
                SyncChipInfoS2C.TYPE,
                SyncChipInfoS2C.CODEC,
                SyncChipInfoS2C::handle
        );
        event.playToClient(
                SyncFetchAnswerS2C.TYPE,
                SyncFetchAnswerS2C.CODEC,
                SyncFetchAnswerS2C::handle
        );
        event.playToClient(
                EtherStreamCreateS2C.TYPE,
                EtherStreamCreateS2C.CODEC,
                EtherStreamCreateS2C::handle
        );
        event.playToClient(
                EtherStreamSetDyingS2C.TYPE,
                EtherStreamSetDyingS2C.CODEC,
                EtherStreamSetDyingS2C::handle
        );
        event.playToClient(
                EtherStreamUpdateS2C.TYPE,
                EtherStreamUpdateS2C.CODEC,
                EtherStreamUpdateS2C::handle
        );
        event.playToClient(
                EtherStreamSyncDataS2C.TYPE,
                EtherStreamSyncDataS2C.CODEC,
                EtherStreamSyncDataS2C::handle
        );
        event.playToClient(
                SyncPluginTipsS2C.TYPE,
                SyncPluginTipsS2C.CODEC,
                SyncPluginTipsS2C::handle
        );


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
        event.playToServer(
                UncarryC2S.TYPE,
                UncarryC2S.CODEC,
                wrapWithPlayer(UncarryC2S::handle)
        );
    }


    private static final String PROTOCOL_VERSION = "1";

    @EventBusSubscriber
    public static class Server {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            commonMsg(event.registrar(PROTOCOL_VERSION));
        }
    }

    private static <T extends CustomPacketPayload> IPayloadHandler<@NotNull T> wrapWithPlayer(BiConsumer<T, Player> t) {
        return (d, c) -> {
            c.enqueueWork(() -> t.accept(d, c.player()));
        };
    }
}
