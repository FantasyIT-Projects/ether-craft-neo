package studio.fantasyit.ether_craft.stream.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamSetDyingS2C;

import java.util.*;

public class ClientVESHData {

    public static class ClientVESHEntry {
        final Map<Integer, ClientStreamEntry> streams = new HashMap<>();
    }

    private final Map<PosDir, ClientVESHEntry> entries = new HashMap<>();

    public void handleCreate(EtherStreamCreateS2C msg) {
        ClientVESHEntry entry = entries.computeIfAbsent(msg.posDir(), k -> new ClientVESHEntry());
        if (!entry.streams.containsKey(msg.streamId())) {
            entry.streams.put(msg.streamId(), new ClientStreamEntry(msg));
        }
    }

    public void handleDying(EtherStreamSetDyingS2C msg) {
        ClientVESHEntry entry = entries.computeIfAbsent(msg.posDir(), k -> new ClientVESHEntry());
        Set<Integer> seen = new HashSet<>();

        for (int id : msg.entries()) {
            ClientStreamEntry current = entry.streams.get(id);
            if (current == null) return;
            if (current.label != null) {
                current.deathTick = 0;
            }
        }

        // Remove streams not in this update batch
        streams.entrySet().removeIf(e -> !seen.contains(e.getKey()));

        // Remove empty entries
        if (entry.streams.isEmpty()) {
            entries.remove(msg.posDir());
        }
    }

    public Map<PosDir, ClientVESHEntry> getEntries() {
        return entries;
    }

    public static ClientVESHData get() {
        Level level = Minecraft.getInstance().level;
        if (level == null) return new ClientVESHData();
        return CACHE.computeIfAbsent(level, k -> new ClientVESHData());
    }

    private static final Map<Level, ClientVESHData> CACHE = new WeakHashMap<>();
}
