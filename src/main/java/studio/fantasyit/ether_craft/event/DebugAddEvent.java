package studio.fantasyit.ether_craft.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import org.jspecify.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHData;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHEntry;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class DebugAddEvent {
    @SubscribeEvent
    public static void onLevelTick(RegisterDebugEntriesEvent event) {
        event.register(EtherCraft.id("ether_craft_stream"), new EtherCraftDebugEntry());
    }

    public static class EtherCraftDebugEntry implements DebugScreenEntry {
        @Override
        public void display(DebugScreenDisplayer debugScreenDisplayer, @Nullable Level serverLevel, @Nullable LevelChunk levelChunk, @Nullable LevelChunk levelChunk1) {
            Level level = Minecraft.getInstance().level;
            int totalSize = 0;
            ClientVESHData clientVESHData = ClientVESHData.getWithCurrentLevel(level);
            for (ClientVESHEntry entry : clientVESHData.getEntries().values()) {
                totalSize += entry.streams.size();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("EtherStream: ");
            sb.append(totalSize);
            sb.append(" ticking;");
            sb.append("R:");
            sb.append(clientVESHData.lastTickRenderCount).append("|").append(clientVESHData.lastTickParticleCount);
            debugScreenDisplayer.addLine(sb.toString());
        }
    }
}
