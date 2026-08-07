package studio.fantasyit.ether_craft.stream.cap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.BlockRegistry;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

public class EtherStreamTransformGlassCapability implements IStreamCapability{
    public static Identifier ID = EtherCraft.id("ether_stream_transform_glass");
    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void tick(IEtherStreamLike streamEntity) {
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
    public void onDestroy(IEtherStreamLike streamEntity, @Nullable HitResult hitResult) {

    }

    @Override
    public void serialize(ValueOutput output) {

    }

    @Override
    public void deserialize(ValueInput input) {

    }

    @Override
    public void runIntoNewBlock(IEtherStreamLike streamEntity,@Nullable BlockPos oldPos, @Nullable BlockState oldState, BlockPos newPos, BlockState newState) {
        if (newState.is(Blocks.GLASS)) {
            if (streamEntity.level().getRandom().nextDouble() <= Config.etherStreamGlassTransformChance)
                streamEntity.level().setBlockAndUpdate(newPos, BlockRegistry.ETHER_GLASS.get().defaultBlockState());
        }
    }
}
