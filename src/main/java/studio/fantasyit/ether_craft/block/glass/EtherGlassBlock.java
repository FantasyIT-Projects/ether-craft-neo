package studio.fantasyit.ether_craft.block.glass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.client.key.EtherGlassKeyHandler;
import studio.fantasyit.ether_craft.register.BlockRegistry;

import java.util.List;

public class EtherGlassBlock extends TransparentBlock {
    public EtherGlassBlock(Identifier identifier) {
        super(Properties.of()
                .strength(1f)
                .sound(SoundType.GLASS)
                .dynamicShape()
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false)
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .setId(ResourceKey.create(Registries.BLOCK, identifier)));
    }

    @Override
    protected @NotNull List<ItemStack> getDrops(@NotNull BlockState state, @NotNull LootParams.Builder params) {
        return List.of(new ItemStack(BlockRegistry.ETHER_GLASS.get()));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (FMLEnvironment.getDist() != Dist.DEDICATED_SERVER)
            if (EtherGlassKeyHandler.isAltThroughGlassDown())
                return Shapes.empty();
        return super.getShape(state, level, pos, context);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }
}
