package studio.fantasyit.ether_craft.block.base;

import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;

import java.util.List;

public abstract class BaseBlock extends Block implements EntityBlock {
    public BaseBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> be) {
        if (level.isClientSide()) return null;
        return (lvl, pos, st, blockEntity) -> {
            if (blockEntity instanceof ITickable mbe)
                mbe.tickServer();
        };
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState p_49232_) {
        return RenderShape.MODEL;
    }


    public abstract Item getDropItem(BlockState state);

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(getDropItem(state));
        if (params.getParameter(LootContextParams.BLOCK_ENTITY) instanceof BlockEntity be) {
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(be.problemPath(), EtherCraft.LOGGER)) {
                TagValueOutput output = TagValueOutput.createWithContext(reporter, params.getLevel().registryAccess());
                be.saveCustomOnly(output);
                be.removeComponentsFromTag(output);
                BlockItem.setBlockEntityData(stack, be.getType(), output);
                stack.applyComponents(be.collectComponents());
            }
        }
        return List.of(stack);
    }
}
