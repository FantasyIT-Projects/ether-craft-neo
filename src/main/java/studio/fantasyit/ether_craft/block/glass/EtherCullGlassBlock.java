package studio.fantasyit.ether_craft.block.glass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.register.BlockRegistry;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class EtherCullGlassBlock extends EtherGlassBlock {
    public static final BooleanProperty CULL_NORTH = BooleanProperty.create("cull_north");
    public static final BooleanProperty CULL_SOUTH = BooleanProperty.create("cull_south");
    public static final BooleanProperty CULL_EAST = BooleanProperty.create("cull_east");
    public static final BooleanProperty CULL_WEST = BooleanProperty.create("cull_west");
    public static final BooleanProperty CULL_UP = BooleanProperty.create("cull_up");
    public static final BooleanProperty CULL_DOWN = BooleanProperty.create("cull_down");

    private static final Map<Direction, BooleanProperty> CULL_PROPS;

    static {
        EnumMap<Direction, BooleanProperty> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, CULL_NORTH);
        map.put(Direction.SOUTH, CULL_SOUTH);
        map.put(Direction.EAST, CULL_EAST);
        map.put(Direction.WEST, CULL_WEST);
        map.put(Direction.UP, CULL_UP);
        map.put(Direction.DOWN, CULL_DOWN);
        CULL_PROPS = Map.copyOf(map);
    }

    public EtherCullGlassBlock(Identifier identifier) {
        super(identifier);
        registerDefaultState(stateDefinition.any()
                .setValue(CULL_NORTH, false)
                .setValue(CULL_SOUTH, false)
                .setValue(CULL_EAST, false)
                .setValue(CULL_WEST, false)
                .setValue(CULL_UP, false)
                .setValue(CULL_DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CULL_NORTH, CULL_SOUTH, CULL_EAST, CULL_WEST, CULL_UP, CULL_DOWN);
    }

    @Override
    public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState, Direction dir) {
        return state.getValue(CULL_PROPS.get(dir));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (itemStack.is(ItemRegistry.WRENCH) && !player.isShiftKeyDown()) {
            BooleanProperty prop = CULL_PROPS.get(hitResult.getDirection().getOpposite());
            level.setBlockAndUpdate(pos, state.cycle(prop));
            return InteractionResult.SUCCESS_SERVER;
        }
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected @NotNull List<ItemStack> getDrops(@NotNull BlockState state, @NotNull LootParams.Builder params) {
        return List.of(new ItemStack(BlockRegistry.ETHER_CULL_GLASS.get()));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        ItemStack stack = new ItemStack(BlockRegistry.ETHER_CULL_GLASS.get());
        if (includeData) {
            stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY
                    .with(CULL_NORTH, state)
                    .with(CULL_SOUTH, state)
                    .with(CULL_EAST, state)
                    .with(CULL_WEST, state)
                    .with(CULL_UP, state)
                    .with(CULL_DOWN, state));
        }
        return stack;
    }
}
