package studio.fantasyit.ether_craft.block.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import studio.fantasyit.ether_craft.block.base.BaseBlock;
import studio.fantasyit.ether_craft.register.ItemRegistry;

public class EtherProcessFactoryBlock extends BaseBlock {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 4);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public EtherProcessFactoryBlock(Identifier identifier) {
        super(Properties.of()
                .destroyTime(2f)
                .setId(ResourceKey.create(Registries.BLOCK, identifier)));
        registerDefaultState(
                stateDefinition.any()
                        .setValue(LEVEL, 1)
                        .setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new EtherProcessFactoryEntity(blockPos, blockState);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (itemStack.is(ItemRegistry.WRENCH)) {
            @NotNull Direction facing = state.getValue(FACING);
            Direction counterClockWise = facing.getCounterClockWise(Direction.Axis.Y);
            level.setBlockAndUpdate(pos, state.setValue(FACING, counterClockWise));
            return InteractionResult.SUCCESS_SERVER;
        }
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EtherProcessFactoryEntity epfe) {
            player.openMenu(epfe, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Player player = ctx.getPlayer();
        Direction targetFace = ctx.getClickedFace();
        if (Direction.Plane.VERTICAL.test(targetFace)) {
            targetFace = Direction.NORTH;
        }
        if (player != null) {
            Direction[] ds = Direction.orderedByNearest(player);
            for (Direction d : ds) {
                if (Direction.Plane.VERTICAL.test(d)) continue;
                targetFace = d;
                break;
            }
        }
        return defaultBlockState()
                .setValue(FACING, targetFace);
    }

    @Override
    public Item getDropItem(BlockState state) {
        return switch (state.getValue(LEVEL)) {
            case 1 -> ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_1.get();
            case 2 -> ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_2.get();
            case 3 -> ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_3.get();
            case 4 -> ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_4.get();
            default -> throw new IllegalArgumentException("Invalid level: " + state.getValue(LEVEL));
        };
    }
}
