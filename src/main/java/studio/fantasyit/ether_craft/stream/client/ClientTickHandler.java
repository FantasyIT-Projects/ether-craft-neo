package studio.fantasyit.ether_craft.stream.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import studio.fantasyit.ether_craft.EtherCraft;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class ClientTickHandler {
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            ClientVESHData.get().tick();
        }
    }
}
