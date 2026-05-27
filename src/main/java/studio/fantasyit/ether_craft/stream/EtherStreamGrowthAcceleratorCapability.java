package studio.fantasyit.ether_craft.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.entity.EtherStreamEntity;
import studio.fantasyit.ether_craft.register.Tags;

public class EtherStreamGrowthAcceleratorCapability implements IStreamCapability {
    public static final Identifier ID = EtherCraft.id("growth_accelerator_stream");

    private BlockPos lastCatalyzedPos = null;

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public int getConsumption() {
        return 0;
    }

    @Override
    public void tick(EtherStreamEntity streamEntity) {
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
        lastCatalyzedPos = pos;
    }

    @Override
    public boolean hitEntity(ServerLevel level, EtherStreamEntity streamEntity, EntityHitResult hit, Entity entity) {
        return false;
    }

    @Override
    public boolean hitBlock(ServerLevel level, EtherStreamEntity streamEntity, BlockHitResult hit, BlockState blockState) {
        return false;
    }

    @Override
    public void onDestroy(EtherStreamEntity streamEntity) {
    }

    @Override
    public boolean shouldPassThrough(BlockState blockState) {
        return blockState.is(Tags.CROP_ACCELERATABLE);
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
