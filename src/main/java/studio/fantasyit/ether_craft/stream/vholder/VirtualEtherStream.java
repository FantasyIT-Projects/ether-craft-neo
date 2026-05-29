package studio.fantasyit.ether_craft.stream.vholder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VirtualEtherStream implements IEtherStreamLike {
    Vec3 pos;
    Level level;
    Direction direction;
    Vec3 startPos;
    Vec3 motion;

    public boolean markToSyncCreation = false;
    public boolean markToRemove = false;

    List<IStreamCapability> capabilities = new ArrayList<>();
    int ether;
    int streamId;
    int tickCount = 0;

    int labelColor = 0xFFFFFFFF;
    @Nullable
    Component label;

    public VirtualEtherStream(int streamId, int ether, Vec3 pos, Vec3 motion, Level level, Direction direction) {
        this.streamId = streamId;
        this.ether = ether;
        this.pos = pos;
        this.level = level;
        this.direction = direction;
        this.startPos = pos;
        this.motion = motion;
        this.markToSyncCreation = true;
    }


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
        //TODO lower factor
        this.ether = Math.max(0, this.ether - ether);
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
    public void addCapability(IStreamCapability capability) {
        this.capabilities.add(capability);
    }

    public void markDead() {
        if (markToRemove) return;
        for (IStreamCapability cap : capabilities) {
            cap.onDestroy(this);
        }
        markToRemove = true;
    }

    public int getConsumption() {
        double factor = Config.etherStreamConsumptionFactor;
        factor += Config.etherStreamConsumptionByTimeFactor * tickCount;
        double value = Math.ceil(factor * ether);
        for (IStreamCapability cap : capabilities) {
            value += cap.getConsumption();
        }
        return (int) Math.ceil(value);
    }

    public void setLabel(@Nullable Component label, int color) {
        this.label = label;
        this.labelColor = color;
    }
}