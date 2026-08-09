package studio.fantasyit.ether_craft.event;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.stream.vholder.VirtualEtherStreamHolderManager;

@EventBusSubscriber(modid = EtherCraft.MODID)
public class LevelTickHandler {
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        level.getData(AttachmentDataRegistry.INDEX_MAPPING_MANAGER).tick(level);
        VirtualEtherStreamHolderManager.get(level).tick(level);
        if (level.hasData(AttachmentDataRegistry.LEVEL_MUTE_SOURCE)) {
            level.getData(AttachmentDataRegistry.LEVEL_MUTE_SOURCE).tick(level);
        }
    }
}
