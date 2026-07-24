package studio.fantasyit.ether_craft.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.block.base.BaseBlock;
import studio.fantasyit.ether_craft.node.NodePluginManager;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.feature.AbstractDirectionalFeature;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.function.Function;

public class EtherAdaptNodeBlock extends BaseBlock {
    public static final EnumProperty<@NotNull Direction> FACING = BlockStateProperties.FACING;
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 3);;

    public static Function<Identifier, @NotNull EtherAdaptNodeBlock> constructWithLevel(int level) {
        return identifier -> new EtherAdaptNodeBlock(identifier, level);
    }

    public EtherAdaptNodeBlock(Identifier identifier, int level) {
        super(
                Properties.of()
                        .destroyTime(2f)
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
    protected boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(@NotNull BlockState state, @NotNull net.minecraft.world.level.BlockGetter level, @NotNull BlockPos pos, @NotNull Direction direction) {
        if (level.getBlockEntity(pos) instanceof EtherAdaptNodeEntity eane)
            return eane.getAnalogOutputSignal(direction.getOpposite());
        return 0;
    }

    @Override
    protected int getDirectSignal(@NotNull BlockState state, @NotNull net.minecraft.world.level.BlockGetter level, @NotNull BlockPos pos, @NotNull Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction.getOpposite());
    }

    @Override
    protected void neighborChanged(@NotNull BlockState state, Level level, @NotNull BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof EtherAdaptNodeEntity eane)
            eane.onNeighborChanged();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new EtherAdaptNodeEntity(blockPos, blockState);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!player.isShiftKeyDown() && !level.isClientSide() && level.getBlockEntity(pos) instanceof EtherAdaptNodeEntity eane && player.getOffhandItem().is(ItemRegistry.WRENCH)) {
            NodePluginManager.PluginInfo info = NodePluginManager.Instance.getInfoFor(itemStack, NodePluginManager.FEATURE_UPGRADE_TYPE);
            if (info == null)
                info = NodePluginManager.Instance.getInfoFor(itemStack, NodePluginManager.FUNCTION_TYPE);
            if (info != null) {
                EtherPluginUpgradeContainer container;
                if (info.type() == NodePluginManager.PluginType.FUNCTION)
                    container = eane.functionStorage;
                else
                    container = eane.featureUpgradeStorage;

                int sz = container.getContainerSize();
                if (info.type() != NodePluginManager.PluginType.FUNCTION)
                    sz = Math.min(sz, eane.getUpgradeCount());

                int targetSlot = -1;
                for (int i = 0; i < sz; i++) {
                    if (container.getItem(i).isEmpty()) {
                        targetSlot = i;
                        break;
                    }
                }
                if (targetSlot == -1) {
                    player.sendSystemMessage(Component.translatable("message.ether_craft.plugin_slots_full"));
                    return InteractionResult.SUCCESS;
                }

                ItemStack toInstall = itemStack.copy();
                toInstall.setCount(1);
                itemStack.shrink(1);

                container.setItem(targetSlot, toInstall);

                if (info.type() == NodePluginManager.PluginType.FEATURE) {
                    AbstractNodePlugin plugin = container.getPlugin(targetSlot);
                    if (plugin instanceof AbstractDirectionalFeature directional) {
                        directional.direction = hitResult.getDirection();
                    }
                }

                eane.pluginUpdate();
                player.sendSystemMessage(Component.translatable("message.ether_craft.plugin_installed"));
                return InteractionResult.SUCCESS;
            }
        }
        if (itemStack.is(ItemRegistry.WRENCH)) {
            @NotNull Direction facing = state.getValue(FACING);
            Direction counterClockWise = facing.getCounterClockWise(hitResult.getDirection().getAxis());
            if (Direction.Plane.HORIZONTAL.test(facing))
                level.setBlockAndUpdate(pos, state.setValue(FACING, counterClockWise));
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EtherAdaptNodeEntity eane) {
                eane.rotatePluginsByAxis(hitResult.getDirection().getAxis());
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
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
        return RenderShape.MODEL;
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
