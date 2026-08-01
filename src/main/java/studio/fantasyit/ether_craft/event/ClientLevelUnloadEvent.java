package studio.fantasyit.ether_craft.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.client.debug.EtherAdaptNodeUpdateMarker;
import studio.fantasyit.ether_craft.client.debug.EtherStreamSyncMarker;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class ClientLevelUnloadEvent {
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            EtherStreamSyncMarker.clear();
            EtherAdaptNodeUpdateMarker.clear();
        }
    }
}
