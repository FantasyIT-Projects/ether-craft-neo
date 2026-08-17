package studio.fantasyit.ether_craft.stream.client.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import studio.fantasyit.ether_craft.client.debug.EtherStreamSyncMarker;
import studio.fantasyit.ether_craft.network.base.IEtherQuickCreator;
import studio.fantasyit.ether_craft.network.c2s.EtherStreamQuickMissC2S;
import studio.fantasyit.ether_craft.network.s2c.*;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.client.render.ClientVirtualEtherStreamRenderer;
import studio.fantasyit.ether_craft.stream.idx.AutoIndexPosDir;
import studio.fantasyit.ether_craft.stream.client.render.RenderDataUtil;
import studio.fantasyit.ether_craft.stream.client.render.VertexPrecomputer;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamEntryLike;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;
import studio.fantasyit.ether_craft.stream.vholder.VirtualEtherStreamHolder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ClientVESHData {
    private final Object2ObjectOpenHashMap<PosDir, ClientVESHEntry> entries = new Object2ObjectOpenHashMap<>();
    private final List<ClientVESHEntry> entriesIterable = new ArrayList<>();

    // quick 派生链缺失自愈：同一 posDir 的回退请求限流间隔（tick）
    private static final long QUICK_MISS_RESYNC_INTERVAL = 20;
    private final Object2ObjectOpenHashMap<PosDir, Long> lastQuickMissRequest = new Object2ObjectOpenHashMap<>();

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

    private ClientVESHEntry createOrGet(PosDir posDir) {
        if (entries.containsKey(posDir))
            return entries.get(posDir);
        ClientVESHEntry entry = new ClientVESHEntry(posDir);
        entries.put(posDir, entry);
        entriesIterable.add(entry);
        return entry;
    }

    public void handleCreate(PosDir posDir, EtherStreamInitialCreateS2C msg) {
        if (level.get() == null) return;
        ClientVESHEntry entry = createOrGet(posDir);
        if (!entry.streams.containsKey(msg.streamId())) {
            entry.addStream(msg.streamId(), posDir, msg);
            ClientStreamEntry created = entry.streams.get(msg.streamId());
            if (created != null) {
                EtherStreamSyncMarker.record(EtherStreamSyncMarker.Type.CREATE, created.currentPos, msg.posDir().hasIndex(), msg.streamId());
            }
        }
        // 即使 streamId 已存在（batch 历史流 / id 回绕撞车），也推进派生基座，保持与服务端 lastCreateSnapshot 同步
        entry.updateLastCreate(msg);
    }

    public void handleCreate(PosDir posDir, EtherStreamBatchCreateS2C msg) {
        if (level.get() == null) return;
        ClientVESHEntry entry = createOrGet(posDir);
        for (EtherStreamBatchCreateS2C.StreamEntry se : msg.entries()) {
            if (!entry.streams.containsKey(se.streamId())) {
                entry.addStream(se.streamId(), posDir, se, true);
                ClientStreamEntry created = entry.streams.get(se.streamId());
                if (created != null) {
                    EtherStreamSyncMarker.record(EtherStreamSyncMarker.Type.CREATE, created.currentPos, msg.posDir().hasIndex(), se.streamId());
                }
            }
        }
    }

    public void handleQuickCreate(PosDir posDir, IEtherQuickCreator msg) {
        if (level.get() == null) return;
        ClientVESHEntry entry = entries.get(posDir);
        if (entry == null || !entry.hasLast()) {
            // 缺少派生基座（全量创建包曾在索引映射竞速等场景下被丢弃）→ 请求服务端回退全量，自愈 quick 派生链
            requestQuickResync(posDir);
            return;
        }
        if (entry.streams.containsKey((entry.getLastCreateStreamId() + 1) & VirtualEtherStreamHolder.STREAM_ID_MASK)) return;
        IEtherStreamEntryLike quickEntry = entry.getFromLastAndUpdate(msg.tickCount(), msg.ether());
        entry.addStream(quickEntry.streamId(), posDir, quickEntry);
        ClientStreamEntry created = entry.streams.get(quickEntry.streamId());
        if (created != null) {
            // quickWithSyncData：派生后补发与快照不同的 syncedData（label/显示物品/携带实体等）
            if (msg instanceof EtherStreamQuickCreateWithSyncDataS2C quickWithSync) {
                for (IEtherStreamSyncedData data : quickWithSync.syncedData())
                    created.syncedData.put(data.getId(), data);
                created.updateDynamic();
            }
            EtherStreamSyncMarker.record(EtherStreamSyncMarker.Type.QUICK_CREATE, created.currentPos, msg.posDir().hasIndex(), quickEntry.streamId());
        }
    }

    /**
     * 向服务端请求回退全量：本 posDir 的 quick 派生链缺失（entry 不存在或 !hasLast()）。
     * 用全量 posDir 编码（idx=-1），不依赖客户端索引映射（丢包场景下索引可能缺失）。
     * 限流：同一 posDir 至少间隔 QUICK_MISS_RESYNC_INTERVAL tick 才发送一次。
     */
    private void requestQuickResync(PosDir posDir) {
        Level lv = level.get();
        if (lv == null) return;
        long now = lv.getGameTime();
        Long last = lastQuickMissRequest.get(posDir);
        if (last != null && now - last < QUICK_MISS_RESYNC_INTERVAL) return;
        lastQuickMissRequest.put(posDir, now);
        ClientPacketDistributor.sendToServer(new EtherStreamQuickMissC2S(new AutoIndexPosDir(posDir)));
    }

    public void handleUpdate(PosDir posDir, EtherStreamUpdateS2C msg) {
        if (level.get() == null) return;
        ClientVESHEntry entry = createOrGet(posDir);
        for (EtherStreamUpdateS2C.StreamEntry se : msg.entries()) {
            ClientStreamEntry current = entry.streams.get(se.streamId());
            if (current == null || current.isDying || current.removed) continue;
            current.updateFromServer(se.ether(), se.consumerState());
            current.updateDynamic();
            if (EtherStreamSyncMarker.isEnabled()) {
                if (se.consumerState().isPresent())
                    EtherStreamSyncMarker.record(EtherStreamSyncMarker.Type.UPDATE_COST, current.currentPos, msg.posDir().hasIndex(), se.streamId());
                else
                    EtherStreamSyncMarker.record(EtherStreamSyncMarker.Type.UPDATE, current.currentPos, msg.posDir().hasIndex(), se.streamId());
            }
        }
    }

    public void handleDying(PosDir posDir, EtherStreamSetDyingS2C msg) {
        Level lv = level.get();
        if (lv == null) return;
        ClientVESHEntry entry = createOrGet(posDir);
        long levelTime = lv.getGameTime();
        for (int sid : msg.entries()) {
            ClientStreamEntry current = entry.streams.get(sid);
            if (current == null) continue;

            EtherStreamSyncMarker.record(EtherStreamSyncMarker.Type.DELETE, current.currentPos, msg.posDir().hasIndex(), sid);

            if (current.attachedLogic.stream().anyMatch(t -> t.shouldDelayDeath(current))) {
                current.setDying();
                current.deathAtTick = levelTime;
                current.updateDynamic();
            } else {
                current.setRemoved();
            }
        }
    }

    public void handleSync(PosDir posDir, EtherStreamSyncDataS2C etherStreamSyncDataS2C) {
        if (level.get() == null) return;
        ClientVESHEntry ent = createOrGet(posDir);
        if (ent == null) return;
        if (ent.streams.containsKey(etherStreamSyncDataS2C.streamId())) {
            ClientStreamEntry entry = ent.streams.get(etherStreamSyncDataS2C.streamId());
            entry.syncedData.clear();
            for (IEtherStreamSyncedData data : etherStreamSyncDataS2C.data())
                entry.syncedData.put(data.getId(), data);
            entry.updateDynamic();
            EtherStreamSyncMarker.record(EtherStreamSyncMarker.Type.SYNC, entry.currentPos, etherStreamSyncDataS2C.posDir().hasIndex(), etherStreamSyncDataS2C.streamId());
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

    public static ClientVESHData getWithCurrentLevel(Level level) {
        Level ll = lastLevel.get();
        if (ll == null || ll != level || CACHE == null) {
            lastLevel = new WeakReference<>(level);
            CACHE = new ClientVESHData(level);
        }
        return CACHE;
    }

    public static final ClientVESHData DUMMY = new ClientVESHData(null);

    private static WeakReference<Level> lastLevel = new WeakReference<>(null);
    private static ClientVESHData CACHE = null;

    public List<ClientVESHEntry> getEntriesIterable() {
        return entriesIterable;
    }
}
