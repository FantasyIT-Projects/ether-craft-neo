package studio.fantasyit.ether_craft.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import studio.fantasyit.ether_craft.register.ItemRegistry;

@EventBusSubscriber
public class ItemEntityTickEvent {
    @SubscribeEvent
    public static void onItemEntityTick(EntityTickEvent.Post event) {

    }
}
