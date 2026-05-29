package studio.fantasyit.ether_craft.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import studio.fantasyit.ether_craft.node.EtherAdaptNodeUpgradeTabManager;
import studio.fantasyit.ether_craft.node.PluginRenderManager;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientStartupEvent {
    @SubscribeEvent
    public static void init(FMLClientSetupEvent event) {
        EtherAdaptNodeUpgradeTabManager.instance.collect();
        PluginRenderManager.Instance.collect();
    }
}
