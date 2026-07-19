package studio.fantasyit.ether_craft.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.attachment.LevelMuteSources;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class ClientSoundMuteEvent {
    @SubscribeEvent
    public static void onSound(PlaySoundEvent event) {
        SoundInstance sound = event.getOriginalSound();
        if (sound.isRelative()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        if (!level.hasData(AttachmentDataRegistry.LEVEL_MUTE_SOURCE))
            return;
        LevelMuteSources ms = level.getData(AttachmentDataRegistry.LEVEL_MUTE_SOURCE);
        double x = sound.getX();
        double y = sound.getY();
        double z = sound.getZ();
        if (ms.checkMute(x,y,z)) {
            event.setSound(null);  // 阻止播放
        }
    }
}
