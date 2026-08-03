package studio.fantasyit.ether_craft.block.glass;

import net.minecraft.world.level.block.state.BlockState;
import studio.fantasyit.ether_craft.register.BlockRegistry;

public final class EtherGlassUtil {
    private EtherGlassUtil() {
    }

    public static boolean isEtherGlass(BlockState state) {
        return state.is(BlockRegistry.ETHER_GLASS) || state.is(BlockRegistry.ETHER_CULL_GLASS);
    }
}
