package studio.fantasyit.ether_craft.stream.idx;

import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.stream.PosDir;

/**
 * 频率阈值策略（原有行为）：
 * 每个 posdir 计数达到 Config.indexMappingRegisterThreshold 后分配索引，索引永久保留（id 无上限，超出 byte 域时走 varint 转义）。
 */
public class FrequencyThresholdIndexMappingStrategy implements IndexMappingStrategy {
    @Override
    public void recordAndPrepareSend(PosDir pos, IndexMappingManager manager) {
        if (manager.pos2IdDir.containsKey(pos)) return;
        int i = manager.counter.addTo(pos, 1);
        if (i >= Config.indexMappingRegisterThreshold) {
            manager.counter.remove(pos, i);
            manager.assignIfAbsent(pos);
        }
    }

    @Override
    public void tick(IndexMappingManager manager) {
    }
}
