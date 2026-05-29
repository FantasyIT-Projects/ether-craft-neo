package studio.fantasyit.ether_craft.stream.vholder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.stream.EtherConsumer;
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
    public boolean needsEtherSync = false;

    List<IStreamCapability> capabilities = new ArrayList<>();
    public final EtherConsumer consumer = new EtherConsumer();
    int ether;
    int streamId;
    int tickCount = 0;

    int labelColor = 0xFFFFFFFF;
    @Nullable
    Component label;

    VirtualEtherStream() {}

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
        consumeEtherInternal(ether);
        this.needsEtherSync = true;
    }

    @Override
    public void consumeEtherInternal(int ether) {
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
        capability.setConsumer(this.consumer);
    }

    public void markDead() {
        if (markToRemove) return;
        for (IStreamCapability cap : capabilities) {
            cap.onDestroy(this);
        }
        markToRemove = true;
    }

    public int getConsumption() {
        return consumer.getTotalConsumption(ether, tickCount);
    }

    public void setLabel(@Nullable Component label, int color) {
        this.label = label;
        this.labelColor = color;
    }

    VirtualEtherStreamData toData() {
        return new VirtualEtherStreamData(
                streamId,
                pos,
                startPos,
                motion,
                direction,
                ether,
                tickCount,
                label,
                labelColor,
                consumer.toState(),
                new ArrayList<>(capabilities)
        );
    }

    static VirtualEtherStream fromData(ServerLevel level, VirtualEtherStreamData data) {
        VirtualEtherStream ves = new VirtualEtherStream();
        ves.streamId = data.streamId();
        ves.ether = data.ether();
        ves.pos = data.pos();
        ves.startPos = data.startPos();
        ves.motion = data.motion();
        ves.level = level;
        ves.direction = data.direction();
        ves.tickCount = data.tickCount();
        ves.label = data.label();
        ves.labelColor = data.labelColor();
        ves.consumer.fromState(data.consumerState());
        ves.capabilities.addAll(data.capabilities());
        for (IStreamCapability cap : data.capabilities()) {
            cap.setConsumer(ves.consumer);
        }
        return ves;
    }
}