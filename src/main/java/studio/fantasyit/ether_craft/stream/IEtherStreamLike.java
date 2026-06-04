package studio.fantasyit.ether_craft.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;
import studio.fantasyit.ether_craft.stream.data.EtherStreamLabelData;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;

import java.util.Optional;

public interface IEtherStreamLike {
    BlockPos blockPosition();

    Vec3 position();

    Vec3 deltaMovement();

    Level level();

    void consumeEther(int ether);

    void consumeEtherInternal(int ether);

    int getEther();

    Direction getDirection();

    Optional<IStreamCapability> getCapability(Identifier id);

    void addCapability(IStreamCapability capability);

    boolean shouldPassThrough(Entity entity);

    void setSyncedData(IEtherStreamSyncedData data);

    void clearSyncedData(Identifier id);

    @Nullable
    IEtherStreamSyncedData getSyncedData(Identifier id);

    void setRunIntoEtherGlass(boolean isEtherGlass2);
}
