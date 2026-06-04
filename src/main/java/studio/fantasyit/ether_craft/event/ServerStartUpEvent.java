package studio.fantasyit.ether_craft.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import studio.fantasyit.ether_craft.factory.EtherProcessRecipeManager;
import studio.fantasyit.ether_craft.node.NodePluginManager;
import studio.fantasyit.ether_craft.stream.CapabilityFactoryManager;
import studio.fantasyit.ether_craft.stream.client.extra.EtherStreamClientLogicManager;
import studio.fantasyit.ether_craft.stream.data.SyncedEtherStreamDataManager;

@EventBusSubscriber
public class ServerStartUpEvent {
    @SubscribeEvent
    public static void onTagReload(FMLCommonSetupEvent event) {
        NodePluginManager.Instance.collect();
        EtherProcessRecipeManager.collectProvider();
        CapabilityFactoryManager.init();
        SyncedEtherStreamDataManager.collect();
    }
}