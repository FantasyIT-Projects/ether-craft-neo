package studio.fantasyit.ether_craft.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

@EventBusSubscriber
public class LevelTickEvent {
    @SubscribeEvent
    public static void onLevelTick(net.neoforged.neoforge.event.tick.LevelTickEvent.Pre event) {
        event.getLevel().getData(AttachmentDataRegistry.CHAINED_EMITTER_ENTITY_HIT_CACHE).beforeTick();
    }
}
