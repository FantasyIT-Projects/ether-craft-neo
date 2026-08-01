package studio.fantasyit.ether_craft.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.client.debug.EtherAdaptNodeUpdateMarker;
import studio.fantasyit.ether_craft.client.debug.EtherStreamSyncMarker;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class DebugKeyHandler {
    public static final KeyMapping SYNC_MARKER = new KeyMapping(
            "key.ether_craft.sync_marker",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            PlatingTriggerKeyHandler.ETHER_CRAFT_CATEGORY
    );
    public static final KeyMapping ADAPT_NODE_DEBUG = new KeyMapping(
            "key.ether_craft.adapt_node_debug",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            PlatingTriggerKeyHandler.ETHER_CRAFT_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SYNC_MARKER);
        event.register(ADAPT_NODE_DEBUG);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        while (SYNC_MARKER.consumeClick()) {
            EtherStreamSyncMarker.setEnabled(!EtherStreamSyncMarker.isEnabled());
        }
        while (ADAPT_NODE_DEBUG.consumeClick()) {
            EtherAdaptNodeUpdateMarker.setEnabled(!EtherAdaptNodeUpdateMarker.isEnabled());
        }
    }
}
