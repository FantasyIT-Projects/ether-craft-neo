package studio.fantasyit.ether_craft.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.network.c2s.PlatingKeyTriggerC2S;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class PlatingTriggerKeyHandler {
    public static final KeyMapping.Category ETHER_CRAFT_CATEGORY = new KeyMapping.Category(EtherCraft.id("category"));

    public static final KeyMapping PLATING_TRIGGER = new KeyMapping(
            "key.ether_craft.plating_trigger",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            ETHER_CRAFT_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PLATING_TRIGGER);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen != null) return;

        while (PLATING_TRIGGER.consumeClick()) {
            ClientPacketDistributor.sendToServer(new PlatingKeyTriggerC2S());
        }
    }
}
