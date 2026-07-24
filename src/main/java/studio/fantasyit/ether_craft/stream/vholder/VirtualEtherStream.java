package studio.fantasyit.ether_craft.stream.vholder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.register.BlockRegistry;
import studio.fantasyit.ether_craft.stream.EtherConsumer;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class VirtualEtherStream implements IEtherStreamLike {
    Vec3 pos;
    final Level level;
    final Direction direction;
    final float startOffset;
    final float startSpeed;
    final Vec3 motion;
    final PosDir posDir;

    public boolean trackingDirty = true;
    public boolean markToSyncCreation = false;
    public boolean markToRemove = false;
    public boolean markToSyncData = false;
    public boolean needsEtherSync = false;
    public boolean needsEtherConsumerSync = false;
    public boolean runIntoEtherGlass = false;

    List<IStreamCapability> capabilities = new ArrayList<>();
    public final EtherConsumer consumer = new EtherConsumer();
    int ether;
    public int realCanReceiveEther = -1;
    int streamId;
    int tickCount = 0;

    List<IEtherStreamSyncedData> toSyncData = new ArrayList<>();
    final VirtualEtherStreamHolder holder;

    HashSet<Integer> trackingPlayers = new HashSet<>();

    public VirtualEtherStream(int streamId, int ether, PosDir posDir, float startOffset, float startSpeed, Level level, VirtualEtherStreamHolder holder) {
        this.startOffset = startOffset;
        this.startSpeed = startSpeed;
        this.streamId = streamId;
        this.ether = ether;
        this.level = level;
        this.motion = posDir.dir().getUnitVec3().scale(startSpeed);
        this.holder = holder;
        this.markToSyncCreation = true;
        this.pos = posDir.pos().getCenter().add(posDir.dir().getUnitVec3().scale(startOffset));
        this.direction = posDir.dir();
        this.posDir = posDir;
        BlockState blockState = level.getBlockState(BlockPos.containing(this.pos));
        this.setRunIntoEtherGlass(blockState.is(BlockRegistry.ETHER_GLASS));
        this.onRunIntoNewBlock(null, null, blockPosition(), blockState);
        this.needsEtherConsumerSync = false;
        this.needsEtherSync = false;
        if (level instanceof ServerLevel sl) {
            sl.getServer().getPlayerList().getPlayers().forEach(player -> {
                if (player.distanceToSqr(pos) <= Config.etherStreamSyncDistance * Config.etherStreamSyncDistance)
                    trackingPlayers.add(player.getId());
            });
        }
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
    public float getSpeed() {
        return startSpeed;
    }

    @Override
    public int getCanConveyEther() {
        if (realCanReceiveEther != -1 && realCanReceiveEther < ether)
            return realCanReceiveEther;
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
                capability.receiveEther(getCanConveyEther());
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
        if (this.consumer.isDirty()) {
            this.consumer.recompute(this, this.capabilities);
            this.needsEtherSync = true;
            this.needsEtherConsumerSync = true;
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

    public IEtherStreamLike recreate(BlockPos pos, Direction direction, float offset, float speed) {
        PosDir newPosDir = new PosDir(pos, direction);
        IEtherStreamLike stream = level.getData(AttachmentDataRegistry.VESHM).createStream(
                level, newPosDir, ether, offset, speed
        );
        if (stream instanceof VirtualEtherStream newStream) {
            newStream.realCanReceiveEther = realCanReceiveEther;
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
            this.ether = 0;
            this.markToRemove = true;
        }
        return stream;
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

    @Override
    public int tickCount() {
        return tickCount;
    }

    public int getStreamId() {
        return streamId;
    }

    @Override
    public void setRunIntoEtherGlass(boolean isEtherGlass2) {
        runIntoEtherGlass = isEtherGlass2;
        this.consumer.setIsInEtherGlass(isEtherGlass2);
        this.consumer.recompute(this, this.capabilities);
        needsEtherSync = true;
        needsEtherConsumerSync = true;
    }

    VirtualEtherStreamData toData() {
        return new VirtualEtherStreamData(
                streamId,
                pos,
                startOffset,
                startSpeed,
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
                data.posDir(),
                data.startOffset(),
                data.startSpeed(),
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

    public void onRunIntoNewBlock(@Nullable BlockPos oldPos, @Nullable BlockState oldState, BlockPos newPos, BlockState newState) {
        if (oldState != null) {
            boolean isEtherGlass1 = oldState.is(BlockRegistry.ETHER_GLASS);
            boolean isEtherGlass2 = newState.is(BlockRegistry.ETHER_GLASS);
            if (isEtherGlass1 != isEtherGlass2) {
                setRunIntoEtherGlass(isEtherGlass2);
            }
        }
        capabilities.forEach(t -> t.runIntoNewBlock(this, oldPos, oldState, newPos, newState));
    }

    public void addTrackingPlayer(Integer id) {
        trackingPlayers.add(id);
        trackingDirty = true;
    }
}