package studio.fantasyit.ether_craft.stream;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import studio.fantasyit.ether_craft.entity.EtherStreamEntity;

public interface IStreamCapability extends ValueIOSerializable {
    Identifier getId();

    int getConsumption();

    void tick(EtherStreamEntity streamEntity);

    void hitEntity(ServerLevel level, EtherStreamEntity streamEntity, EntityHitResult hit, Entity entity);

    void hitBlock(ServerLevel level, EtherStreamEntity streamEntity, BlockHitResult hit, BlockState blockState);

    void onDestroy(EtherStreamEntity streamEntity);
}
