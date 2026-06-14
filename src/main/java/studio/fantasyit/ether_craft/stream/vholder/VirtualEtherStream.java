package studio.fantasyit.ether_craft.stream.vholder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.register.BlockRegistry;
import studio.fantasyit.ether_craft.stream.EtherConsumer;
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
    public boolean runIntoEtherGlass = false;

    List<IStreamCapability> capabilities = new ArrayList<>();
    public final EtherConsumer consumer = new EtherConsumer();
    int ether;
    int streamId;
    int tickCount = 0;

    List<IEtherStreamSyncedData> toSyncData = new ArrayList<>();
    final VirtualEtherStreamHolder holder;

    public VirtualEtherStream(int streamId, int ether, Vec3 startPos, PosDir posDir, Vec3 motion, Level level, VirtualEtherStreamHolder holder) {
        this.streamId = streamId;
        this.ether = ether;
        this.level = level;
        this.motion = motion;
        this.holder = holder;
        this.markToSyncCreation = true;
        this.startPos = this.pos = startPos;
        this.direction = posDir.dir();
        this.posDir = posDir;
        this.setRunIntoEtherGlass(level.getBlockState(BlockPos.containing(startPos)).is(BlockRegistry.ETHER_GLASS));
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
    public void dirtyConsumer() {
        consumer.markDirty();
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

    public void markDead(@Nullable HitResult hitResult) {
        if (markToRemove) return;
        for (IStreamCapability cap : capabilities) {
            if (!cap.onBeforeDestroy(this, hitResult)) return;
        }
        if (hitResult instanceof BlockHitResult blockHitResult) {
            EtherContainer capability = level.getCapability(EtherContainer.ETHER_CONTAINER, blockHitResult.getBlockPos());
            if (capability != null) {
                capability.receiveEther(getEther());
            }
        }
        for (IStreamCapability cap : capabilities) {
            cap.onDestroy(this, hitResult);
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
        if (!level.isLoaded(this.blockPosition())) {
            return;
        }

        if (this.consumer.isDirty()) {
            this.consumer.recompute(this, this.capabilities);
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
        this.consumeEtherInternal(consumption);

        if (this.getEther() <= 0 || this.tickCount > Config.etherStreamMaxTick) {
            this.markDead(null);
        }
    }

    @Override
    public IEtherStreamLike recreate(Vec3 newPos, Vec3 newMotion) {
        PosDir newPosDir = new PosDir(BlockPos.containing(newPos), Direction.getApproximateNearest(newMotion));
        VirtualEtherStream newStream = new VirtualEtherStream(
                holder.nextId++, ether, newPos, newPosDir, newMotion, level, holder
        );
        newStream.capabilities = this.capabilities;
        this.capabilities = new ArrayList<>();
        for (IStreamCapability cap : newStream.capabilities) {
            cap.setConsumer(newStream.consumer);
        }
        newStream.consumer.fromState(this.consumer.toState());
        newStream.consumer.setIsInEtherGlass(newStream.runIntoEtherGlass);
        newStream.toSyncData = new ArrayList<>(this.toSyncData);
        newStream.tickCount = 0;
        for (IStreamCapability cap : newStream.capabilities) {
            cap.onRecreate(newStream);
        }
        newStream.markToSyncCreation = true;
        holder.streams.add(newStream);
        this.ether = 0;
        this.markToRemove = true;
        return newStream;
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

    @Override
    public void removeInstantly() {
        markDead(null);
    }

    public PosDir getPosDir() {
        return posDir;
    }

    public int getStreamId() {
        return streamId;
    }

    @Override
    public void setRunIntoEtherGlass(boolean isEtherGlass2) {
        runIntoEtherGlass = isEtherGlass2;
        this.consumer.setIsInEtherGlass(isEtherGlass2);
        needsEtherSync = true;
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

    static VirtualEtherStream fromData(ServerLevel level, VirtualEtherStreamData data, VirtualEtherStreamHolder holder) {
        VirtualEtherStream ves = new VirtualEtherStream(
                data.streamId(),
                data.ether(),
                data.startPos(),
                data.posDir(),
                data.motion(),
                level,
                holder
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