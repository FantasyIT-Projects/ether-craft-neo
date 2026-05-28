package studio.fantasyit.ether_craft.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

public interface IStreamCapability extends ValueIOSerializable {
    Identifier getId();

    int getConsumption();

    void tick(IEtherStreamLike streamEntity);

    boolean hitEntity(ServerLevel level, IEtherStreamLike streamEntity, EntityHitResult hit, Entity entity);

    boolean hitBlock(ServerLevel level, IEtherStreamLike streamEntity, BlockHitResult hit, BlockState blockState);

    void onDestroy(IEtherStreamLike streamEntity);

    default void firstTick(IEtherStreamLike etherStreamEntity) {

    }

    default boolean shouldPassThrough(BlockState blockState, ServerLevel level, BlockPos blockPos) {
        return false;
    }
}
