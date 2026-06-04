package studio.fantasyit.ether_craft.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.Config;

public class EtherStreamConsumeModifier {
    public static int modify(int original, int ether, int tickCount, Level level, Vec3 pos, EtherStreamBlockStateReadCache cache) {
        if (cache.isEtherGlass(level, BlockPos.containing(pos))) {
            original = Math.max(0, original - Config.etherGlassPreventConsume);
        }
        return original;
    }
}
