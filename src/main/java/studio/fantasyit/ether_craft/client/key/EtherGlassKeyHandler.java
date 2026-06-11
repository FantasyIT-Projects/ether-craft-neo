package studio.fantasyit.ether_craft.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.KeyMapping;
import studio.fantasyit.ether_craft.EtherCraft;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class EtherGlassKeyHandler {
    public static final KeyMapping ALT_THROUGH_GLASS = new KeyMapping(
            "key.ether_craft.see_through_glass",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            PlatingTriggerKeyHandler.ETHER_CRAFT_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ALT_THROUGH_GLASS);
    }

    public static boolean isAltThroughGlassDown() {
        return ALT_THROUGH_GLASS.isDown();
    }
}
