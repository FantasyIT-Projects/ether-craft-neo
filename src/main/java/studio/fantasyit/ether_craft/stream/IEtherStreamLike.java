package studio.fantasyit.ether_craft.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public interface IEtherStreamLike {
    BlockPos blockPosition();

    Vec3 position();

    Level level();

    void consumeEther(int ether);

    int getEther();

    Direction getDirection();

    Optional<IStreamCapability> getCapability(Identifier id);

    void addCapability(EtherStreamStorageCapability capability);
}
