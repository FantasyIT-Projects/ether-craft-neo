package studio.fantasyit.ether_craft.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.client.FrozenClientInput;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHData;
import studio.fantasyit.ether_craft.stream.client.data.ClientStreamEntry;
import studio.fantasyit.ether_craft.stream.data.EtherStreamCarryingEntityData;

import java.util.Map;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class ClientTickEvent {
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            ClientVESHData.get(mc.level).tick();

            LocalPlayer localPlayer = mc.player;
            if (localPlayer == null) return;

            boolean carried = false;
            for (Map.Entry<studio.fantasyit.ether_craft.stream.PosDir, ClientVESHData.ClientVESHEntry> holderEntry
                    : ClientVESHData.get(mc.level).getEntries().entrySet()) {
                for (ClientStreamEntry streamEntry : holderEntry.getValue().streams.values()) {
                    EtherStreamCarryingEntityData data = (EtherStreamCarryingEntityData)
                            streamEntry.getSyncedData(EtherStreamCarryingEntityData.ID);
                    if (data != null && data.entityUUID().equals(localPlayer.getUUID())) {
                        localPlayer.input = new FrozenClientInput();
                        carried = true;
                        break;
                    }
                }
                if (carried) break;
            }
        }
    }
}
