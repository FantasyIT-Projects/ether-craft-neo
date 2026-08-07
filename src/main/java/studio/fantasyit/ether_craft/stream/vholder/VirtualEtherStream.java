package studio.fantasyit.ether_craft.stream.vholder;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
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
import studio.fantasyit.ether_craft.block.glass.EtherGlassUtil;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.stream.EtherConsumer;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.cap.EtherStreamDisplayTimeCapability;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VirtualEtherStream implements IEtherStreamLike {
    final Level level;
    final Direction direction;
    final float startOffset;
    final float startSpeed;
    final Vec3 motion;
    final PosDir posDir;
    final Vec3 startPos;
    final Vec3 offsetUnit;
    final Vec3 offsetCenter;
    final Vec3i blockOffsetUnit;

    public float currentDistance;
    public boolean trackingDirty = false;
    public boolean trackingInitial = true;
    public boolean markToSyncCreation = false;
    public boolean markToRemove = false;
    public boolean markToSyncData = false;
    public boolean needsEtherSync = false;
    public boolean needsEtherConsumerSync = false;
    public boolean runIntoEtherGlass = false;

    List<IStreamCapability> capabilities = new ArrayList<>();
    Object2ObjectOpenHashMap<Identifier, IStreamCapability> capabilityMap = new Object2ObjectOpenHashMap<>();
    public final EtherConsumer consumer = new EtherConsumer();
    int ether;
    public int realCanReceiveEther = -1;
    int streamId;
    int tickCount = 0;

    List<IEtherStreamSyncedData> toSyncData = new ArrayList<>();
    final VirtualEtherStreamHolder holder;

    IntOpenHashSet trackingPlayers = new IntOpenHashSet();
    IntOpenHashSet lastTrackingPlayers = new IntOpenHashSet();

    public VirtualEtherStream(int streamId, int ether, PosDir posDir, float startOffset, float startSpeed, Level level, VirtualEtherStreamHolder holder) {
        this.startOffset = startOffset;
        this.startSpeed = startSpeed;
        this.streamId = streamId;
        this.ether = ether;
        this.level = level;
        this.blockOffsetUnit = posDir.dir().getUnitVec3i();
        this.offsetUnit = posDir.dir().getUnitVec3();
        this.offsetCenter = posDir.pos().getCenter();
        this.startPos = offsetCenter.add(offsetUnit.scale(startOffset));
        this.motion = offsetUnit.scale(startSpeed);
        this.holder = holder;
        this.markToSyncCreation = true;
        this.direction = posDir.dir();
        this.posDir = posDir;
        this.currentDistance = startOffset + startSpeed * tickCount;
        BlockState blockState = level.getBlockState(BlockPos.containing(startPos));
        this.setRunIntoEtherGlass(EtherGlassUtil.isEtherGlass(blockState));
        this.needsEtherConsumerSync = false;
        this.needsEtherSync = false;
        if (level instanceof ServerLevel sl) {
            sl.getServer().getPlayerList().getPlayers().forEach(player -> {
                if (player.distanceToSqr(startPos) <= Config.etherStreamSyncDistance * Config.etherStreamSyncDistance)
                    trackingPlayers.add(player.getId());
            });
        }
    }

    @Override
    public BlockPos blockPosition() {
        return posDir.pos().offset(this.blockOffsetUnit.multiply(blockDistance()));
    }

    @Override
    public Vec3 position() {
        return offsetCenter.add(offsetUnit.scale(currentDistance));
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
        return Optional.ofNullable(capabilityMap.get(id));
    }

    @Override
    public void addCapability(IStreamCapability capability) {
        this.capabilities.add(capability);
        this.capabilityMap.put(capability.getId(), capability);
        capability.setConsumer(this.consumer);
    }

    @Override
    public boolean shouldPassThrough(Entity entity) {
        for (IStreamCapability cap : capabilities)
            if (cap.shouldPassThrough(entity))
                return true;
        return false;
    }

    public int blockDistance() {
        //从center开始计算的完整长度，当超过0.5，曼哈顿距离会+1，又因为正整数，直接转换到整型即可。
        return (int) (currentDistance + 0.5);
    }
    public int blockDistancePrev() {
        return (int) (currentDistance + 0.5 - startSpeed);
    }

    public boolean isDisplayTime() {
        return getCapability(EtherStreamDisplayTimeCapability.ID).isPresent();
    }

    public void displayTimeTick() {
        this.tickCount++;
        Optional<IStreamCapability> optCap = getCapability(EtherStreamDisplayTimeCapability.ID);
        if (optCap.isPresent() && optCap.get() instanceof EtherStreamDisplayTimeCapability dtCap) {
            BlockPos endPos = dtCap.getEndPos();
            if (endPos != null && blockPosition().equals(endPos)) {
                this.markDead(null);
                return;
            }
        }
        if (this.tickCount > Config.etherStreamMaxTick) {
            this.markDead(null);
        }
    }

    public void markDead(@Nullable HitResult hitResult) {
        if (markToRemove) return;
        if (isDisplayTime()) {
            markToRemove = true;
            return;
        }
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

        if (this.tickCount == 0) {
            for (IStreamCapability cap : this.capabilities) {
                cap.firstTick(this);
            }
            BlockPos pos = blockPosition();
            this.onRunIntoNewBlock(null, null, pos, level.getBlockState(pos));
        }
        this.tickCount++;

        for (IStreamCapability cap : this.capabilities) {
            cap.tick(this, holder.sectorCache());
        }

        int consumption = this.getConsumption();
        this.consumeEtherInternal(consumption);

        if (this.getEther() <= 0 || this.tickCount > Config.etherStreamMaxTick) {
            this.markDead(null);
        }

        if (this.markToRemove) return;

        currentDistance += this.startSpeed;
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
            newStream.capabilityMap = this.capabilityMap;
            this.capabilityMap = new Object2ObjectOpenHashMap<>();
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
        ves.tickCount = data.tickCount();
        ves.consumer.fromState(data.consumerState());
        ves.capabilities.addAll(data.capabilities());
        for (IStreamCapability cap : data.capabilities()) {
            ves.capabilityMap.put(cap.getId(), cap);
            cap.setConsumer(ves.consumer);
        }
        ves.toSyncData = new ArrayList<>(data.toSyncData());
        ves.markToSyncCreation = false;
        return ves;
    }

    public void onRunIntoNewBlock(@Nullable BlockPos oldPos, @Nullable BlockState oldState, BlockPos newPos, BlockState newState) {
        if (isDisplayTime()) return;
        if (oldState != null) {
            boolean isEtherGlass1 = EtherGlassUtil.isEtherGlass(oldState);
            boolean isEtherGlass2 = EtherGlassUtil.isEtherGlass(newState);
            if (isEtherGlass1 != isEtherGlass2) {
                setRunIntoEtherGlass(isEtherGlass2);
            }
        }
        for (IStreamCapability cap : capabilities) {
            cap.runIntoNewBlock(this, oldPos, oldState, newPos, newState);
        }
    }

    public void addTrackingPlayer(int id) {
        trackingPlayers.add(id);
        trackingDirty = true;
    }
}