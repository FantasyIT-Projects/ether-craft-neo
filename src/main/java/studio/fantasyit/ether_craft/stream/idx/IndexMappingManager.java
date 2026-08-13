package studio.fantasyit.ether_craft.stream.idx;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.network.s2c.IndexMappingSyncS2C;
import studio.fantasyit.ether_craft.stream.PosDir;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class IndexMappingManager {
    public Object2IntOpenHashMap<PosDir> pos2IdDir = new Object2IntOpenHashMap<>();
    public Object2IntOpenHashMap<PosDir> counter = new Object2IntOpenHashMap<>();
    public ArrayList<PosDir> toSyncPosDir = new ArrayList<>();
    final ArrayList<Integer> toRemoveIds = new ArrayList<>();
    final ArrayDeque<Integer> freeIds = new ArrayDeque<>();
    final IndexMappingStrategy strategy;
    int maxId = 0;

    public IndexMappingManager() {
        if ("frequency_topk".equals(Config.indexMappingStrategy)) {
            this.strategy = new TopKFrequencyIndexMappingStrategy();
        } else {
            this.strategy = new FrequencyThresholdIndexMappingStrategy();
        }
    }


    public AutoIndexPosDir get(PosDir posDir) {
        if (pos2IdDir.containsKey(posDir)) {
            return new AutoIndexPosDir(pos2IdDir.get(posDir));
        }
        return new AutoIndexPosDir(posDir);
    }

    public void tick(ServerLevel level) {
        if (level.getServer().getTickCount() % Config.indexMappingDecayInterval == 0) {
            for (PosDir posDir : counter.keySet()) {
                counter.addTo(posDir, -1);
            }
            counter.object2IntEntrySet().removeIf(t -> t.getIntValue() <= 0);
            strategy.tick(this);
        }

        if (!toSyncPosDir.isEmpty() || !toRemoveIds.isEmpty()) {
            List<IndexMappingSyncS2C.Entry> entries = new ArrayList<>();
            for (PosDir posDir : toSyncPosDir) {
                entries.add(new IndexMappingSyncS2C.Entry(pos2IdDir.get(posDir), posDir));
            }
            toSyncPosDir.clear();
            List<Integer> removals = new ArrayList<>(toRemoveIds);
            toRemoveIds.clear();
            PacketDistributor.sendToPlayersInDimension(level, new IndexMappingSyncS2C(entries, removals));
        }
    }

    public void syncToPlayer(ServerPlayer player) {
        List<IndexMappingSyncS2C.Entry> entries = new ArrayList<>();
        for (var entry : pos2IdDir.object2IntEntrySet()) {
            entries.add(new IndexMappingSyncS2C.Entry(entry.getIntValue(), entry.getKey()));
        }
        if (!entries.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new IndexMappingSyncS2C(entries, List.of()));
        }
    }

    public void recordAndPrepareSend(PosDir pos) {
        strategy.recordAndPrepareSend(pos, this);
    }

    /**
     * 为 posdir 分配索引（若尚未分配）：优先复用回收的 id，否则取新 id。
     */
    public void assignIfAbsent(PosDir pos) {
        if (pos2IdDir.containsKey(pos)) return;
        int id = freeIds.isEmpty() ? maxId++ : freeIds.pollFirst();
        pos2IdDir.put(pos, id);
        toSyncPosDir.add(pos);
    }

    /**
     * 撤销 posdir 的索引并回收 id，同时登记删除同步。
     */
    public void unassign(PosDir pos) {
        if (!pos2IdDir.containsKey(pos)) return;
        int id = pos2IdDir.removeInt(pos);
        freeIds.addLast(id);
        toRemoveIds.add(id);
    }
}
