package studio.fantasyit.ether_craft.stream.vholder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.stream.EtherConsumer;
import studio.fantasyit.ether_craft.stream.EtherStreamConsumeModifier;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VirtualEtherStream implements IEtherStreamLike {
    Vec3 pos;
    final Level level;
    final Direction direction;
    final Vec3 startPos;
    final Vec3 motion;
    final PosDir posDir;

    public boolean markToSyncCreation = false;
    public boolean markToRemove = false;
    public boolean markToSyncData = false;
    public boolean needsEtherSync = false;

    List<IStreamCapability> capabilities = new ArrayList<>();
    public final EtherConsumer consumer = new EtherConsumer();
    int ether;
    int streamId;
    int tickCount = 0;

    List<IEtherStreamSyncedData> toSyncData = new ArrayList<>();

    public VirtualEtherStream(int streamId, int ether, Vec3 startPos, PosDir posDir, Vec3 motion, Level level) {
        this.streamId = streamId;
        this.ether = ether;
        this.level = level;
        this.motion = motion;
        this.markToSyncCreation = true;
        this.startPos = this.pos = startPos;
        this.direction = posDir.dir();
        this.posDir = posDir;
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
    public Vec3 deltaMovement() {
        return motion;
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

    @Override
    public boolean shouldPassThrough(Entity entity) {
        for (IStreamCapability cap : capabilities)
            if (cap.shouldPassThrough(entity))
                return true;
        return false;
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

    @Override
    public void setSyncedData(IEtherStreamSyncedData data) {
        toSyncData.removeIf(d -> d.getId().equals(data.getId()));
        toSyncData.add(data);
        markToSyncData = true;
    }

    public void tick() {
        if (!level.isLoaded(this.blockPosition()))
            return;

        if (this.consumer.isDirty()) {
            this.consumer.recompute(this.capabilities);
            this.needsEtherSync = true;
        }


        if (this.tickCount == 0)
            for (IStreamCapability cap : this.capabilities) {
                cap.firstTick(this);
            }
        this.tickCount++;

        for (IStreamCapability cap : this.capabilities) {
            cap.tick(this);
        }

        int consumption = this.getConsumption();
        consumption = EtherStreamConsumeModifier.modify(consumption, this.ether, this.tickCount, level, this.position());
        this.consumeEtherInternal(consumption);

        if (this.getEther() <= 0 && this.tickCount > Config.etherStreamMaxTick) {
            this.markDead();
        }
    }

    @Override
    public void clearSyncedData(Identifier id) {
        toSyncData.removeIf(d -> d.getId().equals(id));
        markToSyncData = true;
    }

    @Override
    public @Nullable IEtherStreamSyncedData getSyncedData(Identifier id) {
        for (IEtherStreamSyncedData d : toSyncData) {
            if (d.getId().equals(id)) return d;
        }
        return null;
    }

    public PosDir getPosDir() {
        return posDir;
    }

    public int getStreamId() {
        return streamId;
    }

    VirtualEtherStreamData toData() {
        return new VirtualEtherStreamData(
                streamId,
                pos,
                startPos,
                motion,
                posDir,
                ether,
                tickCount,
                consumer.toState(),
                new ArrayList<>(capabilities),
                toSyncData
        );
    }

    static VirtualEtherStream fromData(ServerLevel level, VirtualEtherStreamData data) {
        VirtualEtherStream ves = new VirtualEtherStream(
                data.streamId(),
                data.ether(),
                data.startPos(),
                data.posDir(),
                data.motion(),
                level
        );
        ves.pos = data.pos();
        ves.tickCount = data.tickCount();
        ves.consumer.fromState(data.consumerState());
        ves.capabilities.addAll(data.capabilities());
        for (IStreamCapability cap : data.capabilities()) {
            cap.setConsumer(ves.consumer);
        }
        ves.toSyncData = new ArrayList<>(data.toSyncData());
        return ves;
    }
}