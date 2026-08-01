package studio.fantasyit.ether_craft.stream.idx;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.stream.PosDir;

public class ReverseIndexMappingManager {
    public Int2ObjectOpenHashMap<PosDir> id2PosDir = new Int2ObjectOpenHashMap<>();
    @Nullable
    public PosDir getById(int idx) {
        return id2PosDir.get(idx);
    }
}
