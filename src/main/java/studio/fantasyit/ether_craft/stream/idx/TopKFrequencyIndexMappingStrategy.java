package studio.fantasyit.ether_craft.stream.idx;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import studio.fantasyit.ether_craft.stream.PosDir;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 频次排序策略：
 * 每 REORDER_INTERVAL_TICKS tick 按频次降序取前 TOP_K 个 posdir 作为编码集合，
 * 超出部分从频次最低的开始排除（回收 id），热度回升后可重新进入。
 * 所有被记录的 posdir（含已编码的）都持续计数，维持热度竞争。
 */
public class TopKFrequencyIndexMappingStrategy implements IndexMappingStrategy {
    public static final int TOP_K = AutoIndexPosDir.IDX_LIMIT;
    public static final int REORDER_INTERVAL_TICKS = 100;

    @Override
    public void recordAndPrepareSend(PosDir pos, IndexMappingManager manager) {
        manager.counter.addTo(pos, 1);
    }

    @Override
    public void tick(IndexMappingManager manager) {
        List<Object2IntMap.Entry<PosDir>> list = new ArrayList<>(manager.counter.object2IntEntrySet());
        // 频次降序；同频按 hashCode 稳定排序，避免边界抖动
        list.sort((a, b) -> {
            int cmp = Integer.compare(b.getIntValue(), a.getIntValue());
            if (cmp != 0) return cmp;
            return Integer.compare(a.getKey().hashCode(), b.getKey().hashCode());
        });
        int limit = Math.min(TOP_K, list.size());
        Set<PosDir> desired = new HashSet<>(limit);
        for (int i = 0; i < limit; i++) {
            desired.add(list.get(i).getKey());
        }
        // 先踢出全部落选者（回收 id），再分配新进入者，保证编码集 <= TOP_K
        List<PosDir> toUnassign = new ArrayList<>();
        for (PosDir pos : manager.pos2IdDir.keySet()) {
            if (!desired.contains(pos)) toUnassign.add(pos);
        }
        for (PosDir pos : toUnassign) {
            manager.unassign(pos);
        }
        for (int i = 0; i < limit; i++) {
            manager.assignIfAbsent(list.get(i).getKey());
        }
    }
}
