package studio.fantasyit.ether_craft.block.glass;

import net.minecraft.core.Direction;

import java.util.Arrays;
import java.util.Objects;

public final class EtherGlassState {

    public static final EtherGlassState DEFAULT;

    static {
        var masks = new int[6];
        Arrays.fill(masks, 0b1111);
        var adjacentGlassBlocks = new boolean[6];
        DEFAULT = new EtherGlassState(masks, adjacentGlassBlocks);
    }

    private final int[] masks;
    private final boolean[] adjacentGlassBlocks;

    public EtherGlassState(int[] masks, boolean[] adjacentGlassBlocks) {
        this.masks = masks.clone();
        this.adjacentGlassBlocks = adjacentGlassBlocks.clone();
    }

    public int getMask(Direction side) {
        return masks[side.get3DDataValue()];
    }

    public boolean hasAdjacentGlassBlock(Direction side) {
        return adjacentGlassBlocks[side.get3DDataValue()];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EtherGlassState that)) return false;
        return Arrays.equals(masks, that.masks) && Arrays.equals(adjacentGlassBlocks, that.adjacentGlassBlocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(masks), Arrays.hashCode(adjacentGlassBlocks));
    }
}
