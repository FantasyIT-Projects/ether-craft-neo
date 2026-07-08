package studio.fantasyit.ether_craft.stream.cap;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.vholder.VirtualEtherStream;

public class EtherStreamBounceBackCapability implements IStreamCapability {
    public static final Identifier ID = EtherCraft.id("bounce_back");
    public static final Codec<EtherStreamBounceBackCapability> CODEC = Codec.BOOL.xmap(b -> new EtherStreamBounceBackCapability(), c -> true);

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public boolean onBeforeDestroy(IEtherStreamLike streamEntity, @Nullable HitResult hitResult) {
        if (streamEntity.getEther() > 0 && streamEntity instanceof VirtualEtherStream ves && streamEntity.tickCount() > 1) {
            Vec3 dm = streamEntity.deltaMovement().reverse();
            Vec3 pos = streamEntity.position().add(dm);
            BlockPos createPos = BlockPos.containing(pos);
            double distanceAlongDm = (pos.subtract(createPos.getCenter())).dot(dm) / dm.length();
            ves.recreate(createPos, streamEntity.getDirection().getOpposite(), (float) distanceAlongDm, ves.getSpeed());
            return false;
        }
        return true;
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
}
