package studio.fantasyit.ether_craft.stream.cap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.UnknownNullability;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.Tags;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

public class EtherStreamGrowthAcceleratorCapability implements IStreamCapability {
    public static final Identifier ID = EtherCraft.id("growth_accelerator_stream");

    private BlockPos lastCatalyzedPos = null;

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void tick(@UnknownNullability IEtherStreamLike streamEntity) {
        if (!(streamEntity.level() instanceof ServerLevel level))
            return;

        BlockPos pos = streamEntity.blockPosition();
        if (pos.equals(lastCatalyzedPos))
            return;

        BlockState state = level.getBlockState(pos);
        if (!state.is(Tags.CROP_ACCELERATABLE))
            return;

        int cost = Config.etherStreamGrowthAcceleratorEtherCost;
        if (streamEntity.getEther() < cost)
            return;

        streamEntity.consumeEther(cost);
        state.randomTick(level, pos, level.getRandom());
        if (state.getBlock() instanceof CaveVines && state.getBlock() instanceof BonemealableBlock b)
            b.performBonemeal(level, level.getRandom(), pos, state);

        lastCatalyzedPos = pos;
    }

    @Override
    public boolean hitEntity(ServerLevel level, IEtherStreamLike streamEntity, EntityHitResult hit, Entity entity) {
        return false;
    }

    @Override
    public boolean hitBlock(ServerLevel level, IEtherStreamLike streamEntity, BlockHitResult hit, BlockState blockState) {
        return false;
    }

    @Override
    public void onDestroy(IEtherStreamLike streamEntity) {
    }

    @Override
    public boolean shouldPassThrough(BlockState blockState, ServerLevel level, BlockPos blockPos) {
        return blockState.is(Tags.CROP_ACCELERATABLE) && !blockState.isCollisionShapeFullBlock(level, blockPos);
    }

    @Override
    public void serialize(ValueOutput output) {
        if (lastCatalyzedPos != null) {
            output.store("lastCatalyzedPos", BlockPos.CODEC, lastCatalyzedPos);
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        lastCatalyzedPos = input.read("lastCatalyzedPos", BlockPos.CODEC).orElse(null);
    }
}
