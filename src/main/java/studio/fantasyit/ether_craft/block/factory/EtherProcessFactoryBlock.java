package studio.fantasyit.ether_craft.block.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import studio.fantasyit.ether_craft.block.base.BaseBlock;

public class EtherProcessFactoryBlock extends BaseBlock {
    public EtherProcessFactoryBlock(Identifier identifier) {
        super(Properties.of().setId(ResourceKey.create(Registries.BLOCK, identifier)));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new EtherProcessFactoryEntity(blockPos, blockState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            player.openMenu((EtherProcessFactoryEntity) level.getBlockEntity(pos), pos);
        }
        return InteractionResult.SUCCESS;
    }
}
