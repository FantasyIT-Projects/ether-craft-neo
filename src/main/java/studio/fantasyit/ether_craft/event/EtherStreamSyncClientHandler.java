package studio.fantasyit.ether_craft.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHData;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class EtherStreamSyncClientHandler {
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityLeaveLevelEvent event) {
        ClientVESHData.remove(event.getLevel());
    }
}
