package studio.fantasyit.ether_craft.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import studio.fantasyit.ether_craft.block.base.BaseBlock;
import studio.fantasyit.ether_craft.register.ItemRegistry;

public class EtherAdaptNodeBlock extends BaseBlock {
    public static final EnumProperty<@NotNull Direction> FACING = BlockStateProperties.FACING;

    public EtherAdaptNodeBlock(Identifier identifier) {
        super(
                Properties.of()
                        .setId(ResourceKey.create(Registries.BLOCK, identifier))
        );
        this.registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.UP)
        );
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new EtherAdaptNodeEntity(blockPos, blockState);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (itemStack.is(ItemRegistry.WRENCH)) {
            @NotNull Direction facing = state.getValue(FACING);
            Direction counterClockWise = facing.getCounterClockWise(hitResult.getDirection().getAxis());
            level.setBlockAndUpdate(pos, state.setValue(FACING, counterClockWise));
            if (!facing.equals(counterClockWise))
                return InteractionResult.SUCCESS;
            return InteractionResult.CONSUME;
        }
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            player.openMenu((EtherAdaptNodeEntity) level.getBlockEntity(pos), pos);
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace());
    }
}
