package studio.fantasyit.ether_craft.stream.idx;

import studio.fantasyit.ether_craft.stream.PosDir;

public interface IndexMappingStrategy {
    /**
     * 记录 posdir 出现 count 次，并根据策略决定是否为其分配索引。
     */
    void recordAndPrepareSend(PosDir pos, int count, IndexMappingManager manager);

    /**
     * 在衰减周期（每 Config.indexMappingDecayInterval tick）被调用一次，用于策略的定期维护（如重排）。
     */
    void tick(IndexMappingManager manager);
}
