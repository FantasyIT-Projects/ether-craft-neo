package studio.fantasyit.ether_craft.stream.client.data;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamSetDyingS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamSyncDataS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamUpdateS2C;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.client.extra.EtherStreamClientLogicManager;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;

import java.util.*;

public class ClientVESHData {


    public static class ClientVESHEntry {
        public final Map<Integer, ClientStreamEntry> streams = new HashMap<>();
    }

    private final Map<PosDir, ClientVESHEntry> entries = new HashMap<>();

    public void handleCreate(EtherStreamCreateS2C msg) {
        ClientVESHEntry entry = entries.computeIfAbsent(msg.posDir(), k -> new ClientVESHEntry());
        for (EtherStreamCreateS2C.StreamEntry se : msg.entries()) {
            if (!entry.streams.containsKey(se.streamId())) {
                entry.streams.put(se.streamId(), new ClientStreamEntry(se));
            }
        }
    }

    public void handleUpdate(EtherStreamUpdateS2C msg) {
        ClientVESHEntry entry = entries.computeIfAbsent(msg.posDir(), k -> new ClientVESHEntry());
        for (EtherStreamUpdateS2C.StreamEntry se : msg.entries()) {
            ClientStreamEntry current = entry.streams.get(se.streamId());
            if (current == null || current.isDying || current.removed) continue;
            current.updateFromServer(se.ether(), se.consumerState());
        }
    }

    public void handleDying(EtherStreamSetDyingS2C msg) {
        ClientVESHEntry entry = entries.computeIfAbsent(msg.posDir(), k -> new ClientVESHEntry());
        long levelTime = Minecraft.getInstance().level.getGameTime();
        for (int sid : msg.entries()) {
            ClientStreamEntry current = entry.streams.get(sid);
            if (current == null) continue;

            if (EtherStreamClientLogicManager.shouldDelayDeath(current)) {
                current.setDying();
                current.deathAtTick = levelTime;
            } else {
                current.setRemoved();
            }
        }
    }

    public void handleSync(EtherStreamSyncDataS2C etherStreamSyncDataS2C) {
        ClientVESHEntry ent = entries.get(etherStreamSyncDataS2C.posDir());
        if (ent == null) return;
        if (ent.streams.containsKey(etherStreamSyncDataS2C.streamId())) {
            for (IEtherStreamSyncedData data : etherStreamSyncDataS2C.data())
                ent.streams.get(etherStreamSyncDataS2C.streamId()).setSyncedData(data);
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

    public static ClientVESHData get(Level level) {
        return CACHE.computeIfAbsent(level, k -> new ClientVESHData());
    }

    private static final Map<Level, ClientVESHData> CACHE = new WeakHashMap<>();
}
