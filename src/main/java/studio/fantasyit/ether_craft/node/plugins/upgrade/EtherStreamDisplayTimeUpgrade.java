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
import studio.fantasyit.ether_craft.stream.cap.EtherStreamDisplayTimeCapability;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;

import java.util.Optional;

public class EtherStreamDisplayTimeUpgrade extends AbstractNodePlugin implements IEtherStreamCapabilityProviderPlugin {
    public static final Identifier ID = EtherCraft.id("ether_stream_display_time_upgrade");

    @Nullable
    private BlockPos cachedEndPos;
    private long cachedAtGameTime = -1;

    public EtherStreamDisplayTimeUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void provideCapabilities(IEtherStreamLike entity) {
        Optional<IStreamCapability> existing = entity.getCapability(EtherStreamDisplayTimeCapability.ID);
        if (existing.isPresent()) return;

        @Nullable BlockPos endPos = getCachedEndPos(entity);
        EtherStreamDisplayTimeCapability cap = new EtherStreamDisplayTimeCapability(endPos);
        entity.addCapability(cap);
    }

    private @Nullable BlockPos getCachedEndPos(IEtherStreamLike entity) {
        if (!(nodeEntity.getLevel() instanceof ServerLevel serverLevel)) return null;
        long gameTime = serverLevel.getGameTime();
        if (cachedEndPos != null && gameTime - cachedAtGameTime < Config.etherStreamDisplayTimeCacheTick) {
            return cachedEndPos;
        }
        cachedEndPos = computeEndPos(serverLevel, entity);
        cachedAtGameTime = gameTime;
        return cachedEndPos;
    }

    private @Nullable BlockPos computeEndPos(ServerLevel level, IEtherStreamLike entity) {
        Direction dir = entity.getDirection();
        Vec3 start = entity.position();
        int maxDist = Config.etherStreamMaxTick * Math.max(1, (int) Math.ceil(entity.getSpeed()));
        Vec3 rayEnd = start.add(dir.getUnitVec3().scale(maxDist + 1));

        BlockPos.MutableBlockPos cursor = BlockPos.containing(start).mutable();
        for (int i = 0; i <= maxDist; i++) {
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir() && !state.is(Tags.ETHER_STREAM_PASS_THROUGH)) {
                VoxelShape shape = state.getCollisionShape(level, cursor);
                if (!shape.isEmpty()) {
                    HitResult hit = shape.clip(start, rayEnd, cursor);
                    if (hit != null) {
                        return cursor.immutable();
                    }
                }
            }
            cursor.move(dir);
        }
        return null;
    }
}
