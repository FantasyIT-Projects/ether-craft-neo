package studio.fantasyit.ether_craft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class CheeseBlock extends Block {
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 10, 14);

    public CheeseBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                            @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }
}
