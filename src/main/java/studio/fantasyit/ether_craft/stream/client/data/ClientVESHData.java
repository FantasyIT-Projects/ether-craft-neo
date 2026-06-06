package studio.fantasyit.ether_craft.stream.client.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamSetDyingS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamSyncDataS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamUpdateS2C;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.client.render.ClientVirtualEtherStreamRenderer;
import studio.fantasyit.ether_craft.stream.client.render.RenderDataUtil;
import studio.fantasyit.ether_craft.stream.client.render.VertexPrecomputer;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ClientVESHData {
    private final Object2ObjectOpenHashMap<PosDir, ClientVESHEntry> entries = new Object2ObjectOpenHashMap<>();
    private final List<ClientVESHEntry> entriesIterable = new ArrayList<>();

    private final WeakReference<Level> level;

    //profilers
    public int lastTickRenderCount = 0;
    public int lastTickParticleCount = 0;
    public int[] lastRenderCost = new int[10];
    public int[] renderCost = new int[10];
    public long lastNanos = 0;

    public ClientVESHData(Level level) {
        this.level = new WeakReference<>(level);
    }

    public ClientVESHEntry createOrGet(PosDir posDir) {
        if (entries.containsKey(posDir))
            return entries.get(posDir);
        ClientVESHEntry entry = new ClientVESHEntry(posDir);
        entries.put(posDir, entry);
        entriesIterable.add(entry);
        return entry;
    }

    public void handleCreate(EtherStreamCreateS2C msg) {
        if (level.get() == null) return;
        ClientVESHEntry entry = createOrGet(msg.posDir());
        for (EtherStreamCreateS2C.StreamEntry se : msg.entries()) {
            if (!entry.streams.containsKey(se.streamId())) {
                entry.addStream(se.streamId(), new ClientStreamEntry(se));
            }
        }
    }

    public void handleUpdate(EtherStreamUpdateS2C msg) {
        if (level.get() == null) return;
        ClientVESHEntry entry = createOrGet(msg.posDir());
        for (EtherStreamUpdateS2C.StreamEntry se : msg.entries()) {
            ClientStreamEntry current = entry.streams.get(se.streamId());
            if (current == null || current.isDying || current.removed) continue;
            current.updateFromServer(se.ether(), se.consumerState());
            current.updateDynamic();
        }
    }

    public void handleDying(EtherStreamSetDyingS2C msg) {
        if (level.get() == null) return;
        ClientVESHEntry entry = createOrGet(msg.posDir());
        long levelTime = Minecraft.getInstance().level.getGameTime();
        for (int sid : msg.entries()) {
            ClientStreamEntry current = entry.streams.get(sid);
            if (current == null) continue;

            if (current.attachedLogic.stream().anyMatch(t -> t.shouldDelayDeath(current))) {
                current.setDying();
                current.deathAtTick = levelTime;
                current.updateDynamic();
            } else {
                current.setRemoved();
            }
        }
    }

    public void handleSync(EtherStreamSyncDataS2C etherStreamSyncDataS2C) {
        if (level.get() == null) return;
        ClientVESHEntry ent = createOrGet(etherStreamSyncDataS2C.posDir());
        if (ent == null) return;
        if (ent.streams.containsKey(etherStreamSyncDataS2C.streamId())) {
            ClientStreamEntry entry = ent.streams.get(etherStreamSyncDataS2C.streamId());
            entry.syncedData.clear();
            for (IEtherStreamSyncedData data : etherStreamSyncDataS2C.data())
                entry.syncedData.put(data.getId(), data);
            entry.updateDynamic();
        }
    }

    public void tick() {
        Level lv = this.level.get();
        if (lv == null) {
            return;
        }
        for (var vesh : entriesIterable) {
            vesh.tick(lv);
            if (vesh.streams.isEmpty()) {
                entries.remove(vesh.posDir);
            }
        }
        entriesIterable.removeIf(vesh -> vesh.streams.isEmpty());

        Level localLevel = Minecraft.getInstance().level;
        if (lv == localLevel) {
            List<VertexPrecomputer.EntrySnapshot> snap = RenderDataUtil.buildEntries(this);
            ClientVirtualEtherStreamRenderer.PRECOMPUTER.submitEntries(snap);
        }
    }

    // ==== Render Profilers ====
    public void startRenderStamp() {
        lastNanos = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            lastRenderCost[i] = renderCost[i];
            renderCost[i] = 0;
        }
    }

    public void renderStamp() {
        lastNanos = System.nanoTime();
    }

    public void renderStamp(int target) {
        long l = System.nanoTime();
        renderCost[target] += (int) (l - lastNanos);
        lastNanos = l;
    }

    public Object2ObjectOpenHashMap<PosDir, ClientVESHEntry> getEntries() {
        return entries;
    }

    public static ClientVESHData get(Level level) {
        return CACHE.computeIfAbsent(level, ClientVESHData::new);
    }

    private static final Map<Level, ClientVESHData> CACHE = new WeakHashMap<>();

    public List<ClientVESHEntry> getEntriesIterable() {
        return entriesIterable;
    }
}
