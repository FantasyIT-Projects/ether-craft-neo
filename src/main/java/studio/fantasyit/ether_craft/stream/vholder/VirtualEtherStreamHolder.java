package studio.fantasyit.ether_craft.stream.vholder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;

import java.util.ArrayList;
import java.util.List;

public class VirtualEtherStreamHolder {
    private final Direction direction;
    private final BlockPos pos;
    private final ServerLevel level;
    int activateTick = 5;
    final List<VirtualEtherStream> streams = new ArrayList<>();
    int nextId = 0;

    public VirtualEtherStreamHolder(BlockPos pos, Direction direction, ServerLevel level) {
        this.level = level;
        this.pos = pos;
        this.direction = direction;
    }

    public VirtualEtherStream createStream(int ether, Vec3 pos, Vec3 motion) {
        VirtualEtherStream ves = new VirtualEtherStream(
                nextId++,
                ether,
                pos,
                motion,
                level,
                direction
        );
        streams.add(ves);
        return ves;
    }

    public void tick(ChainedEmitterEntityHitCache cache, PosDir posDir) {
        List<Integer> collectedToCreate = new ArrayList<>();
        List<Integer> collectedToRemove = new ArrayList<>();
        activateTick--;
        for (VirtualEtherStream ves : streams) {
            ves.tickCount++;

            if (ves.tickCount == 1) {
                for (IStreamCapability cap : ves.capabilities) {
                    cap.firstTick(ves);
                }
            }

            if (ves.tickCount > Config.etherStreamMaxTick) {
                ves.markDead();
            }

            for (IStreamCapability cap : ves.capabilities) {
                cap.tick(ves);
            }

            int consumption = ves.getConsumption();
            ves.consumeEther(consumption);

            if (ves.getEther() <= 0) {
                ves.markDead();
            }

            ves.pos = ves.pos.add(ves.motion);

            if (ves.markToRemove)
                collectedToRemove.add(ves.streamId);
            if (ves.markToSyncCreation)
                collectedToCreate.add(ves.streamId);
        }
        //TODO sync
        streams.removeIf(ves -> ves.markToRemove);
    }

    public boolean isDead() {
        return activateTick <= 0 && streams.isEmpty();
    }
}
