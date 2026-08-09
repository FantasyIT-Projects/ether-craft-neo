package studio.fantasyit.ether_craft.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.stream.vholder.VirtualEtherStreamHolderManager;

@EventBusSubscriber(modid = EtherCraft.MODID)
public class EtherStreamSyncHandler {
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getLevel() instanceof ServerLevel level) {
            level.getData(AttachmentDataRegistry.INDEX_MAPPING_MANAGER).syncToPlayer(player);
            VirtualEtherStreamHolderManager mgr = VirtualEtherStreamHolderManager.get(level);
            mgr.syncAndStratTrackingByPlayer(player);
            mgr.clearQuickState(player.getId());
        }
    }
}
