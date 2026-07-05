package studio.fantasyit.ether_craft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;

public class LevelUtil {
    public static boolean isLoadedIgnoreHeight(Level level, BlockPos blockPos) {
        if (!Level.isInValidBoundsHorizontal(blockPos)) {
            return false;
        }
        return level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()));
    }

    public static boolean validBlockPos(Level level, BlockPos blockPos) {
        return level.isInValidBounds(blockPos);
    }
}
