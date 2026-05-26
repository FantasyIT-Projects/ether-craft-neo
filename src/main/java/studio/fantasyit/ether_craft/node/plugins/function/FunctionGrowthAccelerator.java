package studio.fantasyit.ether_craft.node.plugins.function;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.register.Tags;

public class FunctionGrowthAccelerator extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("growth_accelerator");

    public FunctionGrowthAccelerator(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void tickWork() {
        if (!(nodeEntity.getLevel() instanceof ServerLevel level))
            return;
        int range = Config.nodeGrowthAcceleratorRange;
        int etherCost = Config.nodeGrowthAcceleratorEtherCost;

        BlockPos center = nodeEntity.getBlockPos();
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > range)
                        continue;
                    if (dx == 0 && dy == 0 && dz == 0)
                        continue;

                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Tags.CROP_ACCELERATABLE)) {
                        if (nodeEntity.getEther() < etherCost)
                            return;
                        nodeEntity.extractEther(etherCost);
                        state.randomTick(level, pos, level.getRandom());
                    }
                }
            }
        }
    }
}
