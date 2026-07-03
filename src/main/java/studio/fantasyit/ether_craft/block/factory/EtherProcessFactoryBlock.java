package studio.fantasyit.ether_craft.block.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import studio.fantasyit.ether_craft.block.base.BaseBlock;
import studio.fantasyit.ether_craft.register.ItemRegistry;

public class EtherProcessFactoryBlock extends BaseBlock {
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;

    public EtherProcessFactoryBlock(Identifier identifier) {
        super(Properties.of()
                .destroyTime(2f)
                .setId(ResourceKey.create(Registries.BLOCK, identifier)));
        registerDefaultState(
                stateDefinition.any()
                        .setValue(LEVEL, 1)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new EtherProcessFactoryEntity(blockPos, blockState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EtherProcessFactoryEntity epfe) {
            player.openMenu(epfe , pos);
        }
        return InteractionResult.SUCCESS;
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
