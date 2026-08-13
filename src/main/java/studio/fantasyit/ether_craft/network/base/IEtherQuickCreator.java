package studio.fantasyit.ether_craft.network.base;

import studio.fantasyit.ether_craft.stream.idx.AutoIndexPosDir;

import java.util.Optional;

public interface IEtherQuickCreator {
    AutoIndexPosDir posDir();

    /** 可选的 firstTick（初始 tick 计数）覆盖值；空表示沿用客户端缓存 */
    default Optional<Integer> tickCount() {
        return Optional.empty();
    }

    /** 可选的 ether 覆盖值；空表示沿用客户端缓存 */
    default Optional<Integer> ether() {
        return Optional.empty();
    }
}
