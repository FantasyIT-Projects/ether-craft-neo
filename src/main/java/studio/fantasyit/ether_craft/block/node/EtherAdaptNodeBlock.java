package studio.fantasyit.ether_craft.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.base.BaseBlock;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.List;
import java.util.function.Function;

public class EtherAdaptNodeBlock extends BaseBlock {
    public static final EnumProperty<@NotNull Direction> FACING = BlockStateProperties.FACING;
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;

    public static Function<Identifier, @NotNull EtherAdaptNodeBlock> constructWithLevel(int level) {
        return identifier -> new EtherAdaptNodeBlock(identifier, level);
    }

    public EtherAdaptNodeBlock(Identifier identifier, int level) {
        super(
                Properties.of()
                        .setId(ResourceKey.create(Registries.BLOCK, identifier))
        );
        this.registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.UP)
                        .setValue(LEVEL, level)
        );
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(LEVEL);
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
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EtherAdaptNodeEntity eane) {
            player.openMenu(eane.getMenuProvider(null));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace());
    }


    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState p_49232_) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public Item getDropItem(BlockState state) {
        return switch (state.getValue(LEVEL)) {
            case 1 -> ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_1.get();
            case 2 -> ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_2.get();
            case 3 -> ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_3.get();
            default -> throw new IllegalArgumentException("Invalid level: " + state.getValue(LEVEL));
        };
    }
}
