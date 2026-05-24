package studio.fantasyit.ether_craft.util;

import java.util.List;
import java.util.function.Consumer;

public class CollectionUtil {
    public static <T extends List<?>> void fullPermutationIndex(T list, Consumer<int[]> consumer) {
        boolean[] flg = new boolean[list.size()];
        int[] index = new int[list.size()];
        permutationDfs(consumer, 0, index, flg);
    }

    private static <T> void permutationDfs(Consumer<int[]> consumer,
                                           int depth,
                                           int[] index,
                                           boolean[] flg) {
        if (depth == index.length) {
            consumer.accept(index);
            return;
        }
        for (int i = 0; i < index.length; i++) {
            if (flg[i]) continue;
            flg[i] = true;
            index[depth] = i;
            permutationDfs(consumer, depth + 1, index, flg);
            flg[i] = false;
        }
    }
}
