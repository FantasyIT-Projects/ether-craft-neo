package studio.fantasyit.ether_craft.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import studio.fantasyit.ether_craft.factory.EtherProcessRecipeManager;
import studio.fantasyit.ether_craft.node.EtherAdaptNodeUpgradeTabManager;
import studio.fantasyit.ether_craft.node.NodePluginManager;

@EventBusSubscriber
public class ServerStartUpEvent {
    @SubscribeEvent
    public static void onTagReload(FMLCommonSetupEvent event) {
        NodePluginManager.Instance.collect();
        EtherProcessRecipeManager.collectProvider();
    }
}
