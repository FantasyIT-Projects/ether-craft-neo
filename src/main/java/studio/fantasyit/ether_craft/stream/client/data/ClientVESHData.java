package studio.fantasyit.ether_craft.stream.client.data;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamSetDyingS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamSyncDataS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamUpdateS2C;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.stream.EtherStreamBlockStateReadCache;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.client.extra.EtherStreamClientLogicManager;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;

import java.lang.ref.WeakReference;
import java.util.*;

public class ClientVESHData {
    public static final ClientVESHData DUMMY = new ClientVESHData(null);

    public static class ClientVESHEntry {

        public final Map<Integer, ClientStreamEntry> streams = new HashMap<>();
    }

    private final Map<PosDir, ClientVESHEntry> entries = new HashMap<>();
    private final WeakReference<Level> level;
    public int lastTickRenderCount = 0;
    public int lastTickParticleCount = 0;

    public ClientVESHData(Level level) {
        this.level = new WeakReference<>(level);
    }

    public void handleCreate(EtherStreamCreateS2C msg) {
        if (level.get() == null) return;
        ClientVESHEntry entry = entries.computeIfAbsent(msg.posDir(), k -> new ClientVESHEntry());
        for (EtherStreamCreateS2C.StreamEntry se : msg.entries()) {
            if (!entry.streams.containsKey(se.streamId())) {
                entry.streams.put(se.streamId(), new ClientStreamEntry(se));
            }
        }
    }

    public void handleUpdate(EtherStreamUpdateS2C msg) {
        if (level.get() == null) return;
        ClientVESHEntry entry = entries.computeIfAbsent(msg.posDir(), k -> new ClientVESHEntry());
        for (EtherStreamUpdateS2C.StreamEntry se : msg.entries()) {
            ClientStreamEntry current = entry.streams.get(se.streamId());
            if (current == null || current.isDying || current.removed) continue;
            current.updateFromServer(se.ether(), se.consumerState());
        }
    }

    public void handleDying(EtherStreamSetDyingS2C msg) {
        if (level.get() == null) return;
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
        if (level.get() == null) return;
        ClientVESHEntry ent = entries.get(etherStreamSyncDataS2C.posDir());
        if (ent == null) return;
        if (ent.streams.containsKey(etherStreamSyncDataS2C.streamId())) {
            ClientStreamEntry entry = ent.streams.get(etherStreamSyncDataS2C.streamId());
            entry.syncedData.clear();
            for (IEtherStreamSyncedData data : etherStreamSyncDataS2C.data())
                entry.syncedData.put(data.getId(), data);
        }
    }

    public void tick() {
        Level lv = this.level.get();
        if (lv == null) {
            return;
        }
        EtherStreamBlockStateReadCache data = lv.getData(AttachmentDataRegistry.ESBS_CACHE);
        data.clearCache();
        List<PosDir> toRemove = new ArrayList<>();
        for (var entry : entries.entrySet()) {
            ClientVESHEntry vesh = entry.getValue();
            vesh.streams.values().forEach(t -> t.tick(lv, data));
            vesh.streams.values().stream().filter(e -> e.removed).forEach(EtherStreamClientLogicManager::onDestroy);
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
        return CACHE.computeIfAbsent(level, ClientVESHData::new);
    }

    private static final Map<Level, ClientVESHData> CACHE = new WeakHashMap<>();
}
