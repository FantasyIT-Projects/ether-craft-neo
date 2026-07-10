package studio.fantasyit.ether_craft.plating.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import studio.fantasyit.ether_craft.register.SoundRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CoyoteTimeAudioPlayer {
    private static final Map<UUID, SoundInstance> PLAYING = new HashMap<>();

    public static void start(Player player) {
        if (PLAYING.containsKey(player.getUUID())) return;
        SoundInstance instance = new SimpleSoundInstance(
                SoundRegistry.COYOTE_TIME_LOOP.get().location(),
                SoundSource.PLAYERS,
                1.0F, 1.0F,
                player.getRandom(),
                true, 0,
                SoundInstance.Attenuation.LINEAR,
                player.getX(), player.getY(), player.getZ(),
                false
        );
        Minecraft.getInstance().getSoundManager().play(instance);
        PLAYING.put(player.getUUID(), instance);
    }

    public static void stop(Player player) {
        SoundInstance instance = PLAYING.remove(player.getUUID());
        if (instance != null) {
            Minecraft.getInstance().getSoundManager().stop(instance);
        }
    }
}