package studio.fantasyit.ether_craft.stream.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamSetDyingS2C;
import studio.fantasyit.ether_craft.stream.PosDir;

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

        for (EtherStreamSetDyingS2C.StreamEntry se : msg.entries()) {
            seen.add(se.streamId());
            ClientStreamEntry current = entry.streams.get(se.streamId());
            if (current == null) continue;

            current.startTickCount = se.tickCount();
            current.ether = se.ether();

            if (se.isDying() && !current.isDying) {
                current.setDying(true);
            }
            if (se.isDead() && !se.isDying()) {
                current.removed = true;
            }
        }

        // Remove streams not in this batch (expired without fanfare, and not currently dying)
        entry.streams.entrySet().removeIf(e -> !seen.contains(e.getKey()) && !e.getValue().isDying);

        // Remove empty entries
        if (entry.streams.isEmpty()) {
            entries.remove(msg.posDir());
        }
    }

    public void tick() {
        List<PosDir> toRemove = new ArrayList<>();
        for (var entry : entries.entrySet()) {
            ClientVESHEntry vesh = entry.getValue();
            vesh.streams.values().forEach(ClientStreamEntry::tick);
            vesh.streams.values().removeIf(e -> e.removed);
            if (vesh.streams.isEmpty()) {
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(entries::remove);
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
