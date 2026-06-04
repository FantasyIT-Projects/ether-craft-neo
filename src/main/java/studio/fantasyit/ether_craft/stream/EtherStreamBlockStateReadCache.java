package studio.fantasyit.ether_craft.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import studio.fantasyit.ether_craft.register.BlockRegistry;

import java.util.HashMap;

public class EtherStreamBlockStateReadCache {
    private HashMap<BlockPos, Boolean> etherGlassCache = new HashMap<>();

    public void clearCache() {
        etherGlassCache.clear();
    }

    public boolean isEtherGlass(Level level, BlockPos pos) {
        if (etherGlassCache.containsKey(pos))
            return etherGlassCache.get(pos);
        boolean result = level.getBlockState(pos).is(BlockRegistry.ETHER_GLASS);
        etherGlassCache.put(pos, result);
        return result;
    }
}
