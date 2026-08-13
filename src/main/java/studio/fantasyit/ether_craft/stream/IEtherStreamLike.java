package studio.fantasyit.ether_craft.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;
import studio.fantasyit.ether_craft.stream.data.StreamExtraProperty;

import java.util.List;
import java.util.Optional;

public interface IEtherStreamLike {
    BlockPos blockPosition();

    Vec3 position();

    Vec3 deltaMovement();
    Vec3 getLastPosition();

    Level level();

    void consumeEther(int ether);

    void consumeEtherInternal(int ether);

    void dirtyConsumer();

    int getEther();

    float getSpeed();

    Direction getDirection();

    Optional<IStreamCapability> getCapability(Identifier id);

    void addCapability(IStreamCapability capability);

    boolean shouldPassThrough(Entity entity);

    void setSyncedData(IEtherStreamSyncedData data);

    void clearSyncedData(Identifier id);

    @Nullable
    IEtherStreamSyncedData getSyncedData(Identifier id);

    void setRunIntoEtherGlass(boolean isEtherGlass2);

    StreamExtraProperty getExtraProperty();

    void removeInstantly();

    int getStreamId();

    PosDir getPosDir();

    int tickCount();

    int getCanConveyEther();

    List<Entity> getEntities(AABB aabb);
    List<Entity> getEntitiesInCurrentPos();

    default boolean isInFullBlock() {
        return false;
    }

    default void setSkip(BlockPos pos) {
    }
}
