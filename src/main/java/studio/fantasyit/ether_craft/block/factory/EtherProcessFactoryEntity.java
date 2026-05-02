package studio.fantasyit.ether_craft.block.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import studio.fantasyit.ether_craft.block.base.BaseIOBlockEntity;

public class EtherProcessFactoryEntity extends BaseIOBlockEntity {
    public EtherProcessFactoryEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState) {
        super(type, worldPosition, blockState, input, internal, outputs);
    }
}
