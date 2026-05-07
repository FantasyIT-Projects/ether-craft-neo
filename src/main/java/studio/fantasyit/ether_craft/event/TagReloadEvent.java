package studio.fantasyit.ether_craft.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import studio.fantasyit.ether_craft.node.NodePluginManager;

@EventBusSubscriber
public class TagReloadEvent {
    @SubscribeEvent
    public static void onTagReload(TagsUpdatedEvent event) {
        NodePluginManager.Instance.collect();
    }
}
