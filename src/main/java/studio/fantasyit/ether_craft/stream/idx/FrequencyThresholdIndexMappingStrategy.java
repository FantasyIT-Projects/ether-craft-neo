package studio.fantasyit.ether_craft.stream.idx;

import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.stream.PosDir;

/**
 * 频率阈值策略（原有行为）：
 * 每个 posdir 计数达到 Config.indexMappingRegisterThreshold 后分配索引，索引永久保留（id 无上限，超出 byte 域时走 varint 转义）。
 * 晋升（assignIfAbsent）统一在 tick() 中进行：INDEX.tick 先于 VESHM.tick，保证「晋升 + toSyncPosDir 广播」
 * 永远先于同 tick 的创建包到达客户端，杜绝「创建包先于索引映射」导致的 resolve(null) 丢包。
 */
public class FrequencyThresholdIndexMappingStrategy implements IndexMappingStrategy {
    @Override
    public void recordAndPrepareSend(PosDir pos, int count, IndexMappingManager manager) {
        if (manager.pos2IdDir.containsKey(pos)) return;
        manager.counter.addTo(pos, count);
    }

    @Override
    public void tick(IndexMappingManager manager) {
        // 统一晋升：达到阈值的 posDir 分配索引；分配后移除计数，避免重复晋升
        manager.counter.object2IntEntrySet().removeIf(e -> {
            if (e.getIntValue() >= Config.indexMappingRegisterThreshold) {
                manager.assignIfAbsent(e.getKey());
                return true;
            }
            return false;
        });
    }
}
