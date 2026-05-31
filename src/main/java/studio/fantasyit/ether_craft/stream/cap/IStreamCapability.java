package studio.fantasyit.ether_craft.stream.cap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import studio.fantasyit.ether_craft.stream.EtherConsumer;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

public interface IStreamCapability extends ValueIOSerializable {
    Identifier getId();

    default void getConsumption(EtherConsumer consumer) {}

    default void setConsumer(EtherConsumer consumer) {}

    void tick(IEtherStreamLike streamEntity);

    boolean hitEntity(ServerLevel level, IEtherStreamLike streamEntity, EntityHitResult hit, Entity entity);

    boolean hitBlock(ServerLevel level, IEtherStreamLike streamEntity, BlockHitResult hit, BlockState blockState);

    void onDestroy(IEtherStreamLike streamEntity);

    default void firstTick(IEtherStreamLike etherStreamEntity) {

    }

    default boolean shouldPassThrough(BlockState blockState, ServerLevel level, BlockPos blockPos) {
        return false;
    }

    default boolean shouldPassThrough(Entity entity) {
        return false;
    }
}
