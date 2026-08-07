package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.IEtherStreamCapabilityProviderPlugin;
import studio.fantasyit.ether_craft.register.Tags;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

public class EtherStreamDisplayTimeUpgrade extends AbstractNodePlugin implements IEtherStreamCapabilityProviderPlugin {
    public static final Identifier ID = EtherCraft.id("ether_stream_display_time_upgrade");

    @Nullable
    private Float cachedMaxDistance;
    private long cachedAtGameTime = -1;

    public EtherStreamDisplayTimeUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void provideCapabilities(IEtherStreamLike entity) {
        @Nullable Float maxDistance = getCachedMaxDistance(entity);
        entity.setDisplayTime(true);
        if (maxDistance != null) {
            entity.setMaxDistance(maxDistance);
        }
    }

    private @Nullable Float getCachedMaxDistance(IEtherStreamLike entity) {
        if (!(nodeEntity.getLevel() instanceof ServerLevel serverLevel)) return null;
        long gameTime = serverLevel.getGameTime();
        if (cachedMaxDistance != null && gameTime - cachedAtGameTime < Config.etherStreamDisplayTimeCacheTick) {
            return cachedMaxDistance;
        }
        cachedMaxDistance = computeMaxDistance(serverLevel, entity);
        cachedAtGameTime = gameTime;
        return cachedMaxDistance;
    }

    private @Nullable Float computeMaxDistance(ServerLevel level, IEtherStreamLike entity) {
        Direction dir = entity.getDirection();
        Vec3 dirUnit = dir.getUnitVec3();
        Vec3 start = entity.position();
        int maxDist = Config.etherStreamMaxTick * Math.max(1, (int) Math.ceil(entity.getSpeed()));
        Vec3 rayEnd = start.add(dirUnit.scale(maxDist + 1));
        Vec3 offsetCenter = entity.getPosDir().pos().getCenter();

        BlockPos.MutableBlockPos cursor = BlockPos.containing(start).mutable();
        for (int i = 0; i <= maxDist; i++) {
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir() && !state.is(Tags.ETHER_STREAM_PASS_THROUGH)) {
                VoxelShape shape = state.getCollisionShape(level, cursor);
                if (!shape.isEmpty()) {
                    HitResult hit = shape.clip(start, rayEnd, cursor);
                    if (hit != null) {
                        Vec3 hitLoc = hit.getLocation();
                        return (float) hitLoc.subtract(offsetCenter).dot(dirUnit);
                    }
                }
            }
            cursor.move(dir);
        }
        return null;
    }
}
