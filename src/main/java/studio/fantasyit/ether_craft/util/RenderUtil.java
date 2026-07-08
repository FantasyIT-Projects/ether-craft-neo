package studio.fantasyit.ether_craft.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

public class RenderUtil {
    public static void dirtyBlockPos(BlockPos pos) {
        Minecraft.getInstance().levelRenderer.setSectionDirty(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getY()),
                SectionPos.blockToSectionCoord(pos.getZ())
        );
    }
}
