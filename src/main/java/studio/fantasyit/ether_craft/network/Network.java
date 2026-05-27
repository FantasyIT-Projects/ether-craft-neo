package studio.fantasyit.ether_craft.network;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import studio.fantasyit.ether_craft.network.s2c.*;

import static studio.fantasyit.ether_craft.network.NetworkClient.clientMsg;

public class Network {
    private static void commonMsg(PayloadRegistrar  event){
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
    }


    private static final String PROTOCOL_VERSION = "1";
    @EventBusSubscriber(value = Dist.DEDICATED_SERVER)
    public static class Server{
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            commonMsg(event.registrar(PROTOCOL_VERSION));
        }
    }
    @EventBusSubscriber(value = Dist.CLIENT)
    public static class Client{
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar r = event.registrar(PROTOCOL_VERSION);
            commonMsg(r);
            clientMsg(r);
        }
    }
}
