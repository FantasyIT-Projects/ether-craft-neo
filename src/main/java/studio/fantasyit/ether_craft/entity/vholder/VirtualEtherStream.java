package studio.fantasyit.ether_craft.entity.vholder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.stream.EtherStreamStorageCapability;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.IStreamCapability;

import java.util.List;
import java.util.Optional;

public class VirtualEtherStream implements IEtherStreamLike {
    Vec3 pos;
    Level level;
    Direction direction;
    int ether;
    List<IStreamCapability> capabilities;


    @Override
    public BlockPos blockPosition() {
        return BlockPos.containing(pos);
    }

    @Override
    public Vec3 position() {
        return pos;
    }

    @Override
    public Level level() {
        return level;
    }

    @Override
    public void consumeEther(int ether) {
        this.ether -= ether;
    }

    @Override
    public int getEther() {
        return ether;
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    @Override
    public Optional<IStreamCapability> getCapability(Identifier id) {
        return capabilities.stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    @Override
    public void addCapability(EtherStreamStorageCapability capability) {
        this.capabilities.add(capability);
    }
}
