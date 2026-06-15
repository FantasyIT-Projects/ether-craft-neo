package studio.fantasyit.ether_craft.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
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

    public static final KeyMapping PLATING_HEAD_TRIGGER = new KeyMapping(
            "key.ether_craft.plating_head_trigger",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            ETHER_CRAFT_CATEGORY
    );

    public static final KeyMapping PLATING_CHEST_TRIGGER = new KeyMapping(
            "key.ether_craft.plating_chest_trigger",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            ETHER_CRAFT_CATEGORY
    );

    public static final KeyMapping PLATING_LEGS_TRIGGER = new KeyMapping(
            "key.ether_craft.plating_legs_trigger",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            ETHER_CRAFT_CATEGORY
    );

    public static final KeyMapping PLATING_FEET_TRIGGER = new KeyMapping(
            "key.ether_craft.plating_feet_trigger",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            ETHER_CRAFT_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PLATING_HEAD_TRIGGER);
        event.register(PLATING_CHEST_TRIGGER);
        event.register(PLATING_LEGS_TRIGGER);
        event.register(PLATING_FEET_TRIGGER);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen != null) return;

        while (PLATING_HEAD_TRIGGER.consumeClick()) {
            ClientPacketDistributor.sendToServer(new PlatingKeyTriggerC2S(EquipmentSlot.HEAD));
        }
        while (PLATING_CHEST_TRIGGER.consumeClick()) {
            ClientPacketDistributor.sendToServer(new PlatingKeyTriggerC2S(EquipmentSlot.CHEST));
        }
        while (PLATING_LEGS_TRIGGER.consumeClick()) {
            ClientPacketDistributor.sendToServer(new PlatingKeyTriggerC2S(EquipmentSlot.LEGS));
        }
        while (PLATING_FEET_TRIGGER.consumeClick()) {
            ClientPacketDistributor.sendToServer(new PlatingKeyTriggerC2S(EquipmentSlot.FEET));
        }
    }
}
