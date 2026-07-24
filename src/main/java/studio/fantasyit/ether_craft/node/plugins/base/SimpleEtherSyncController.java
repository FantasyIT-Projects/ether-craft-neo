package studio.fantasyit.ether_craft.node.plugins.base;

import java.util.function.BiFunction;

public class SimpleEtherSyncController {
    private final BiFunction<Long, Long, Integer> valueGetter;
    private int lastLevelValue = -1;

    public SimpleEtherSyncController(BiFunction<Long, Long, Integer> valueGetter) {
        this.valueGetter = valueGetter;
    }

    public boolean predicate(long ether, long maxEther) {
        int newVal = valueGetter.apply(ether, maxEther);
        if (lastLevelValue != newVal) {
            lastLevelValue = newVal;
            return true;
        }
        return false;
    }
}
