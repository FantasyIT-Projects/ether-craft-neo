package studio.fantasyit.ether_craft.entity.vholder;

import net.minecraft.core.Direction;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.IStreamCapability;

import java.util.ArrayList;
import java.util.List;

public class VirtualEtherStreamHolder {
    final Direction direction;
    int activateTick = 5;
    final List<VirtualEtherStream> streams = new ArrayList<>();
    int nextId = 0;

    public VirtualEtherStreamHolder(Direction direction) {
        this.direction = direction;
    }

    public VirtualEtherStream createStream(int ether, net.minecraft.world.phys.Vec3 pos, net.minecraft.world.phys.Vec3 motion) {
        VirtualEtherStream ves = new VirtualEtherStream();
        ves.pos = pos;
        ves.motion = motion;
        ves.startPos = pos;
        ves.direction = this.direction;
        ves.ether = ether;
        ves.streamId = nextId++;
        streams.add(ves);
        return ves;
    }

    public void tick(ChainedEmitterEntityHitCache cache, PosDir posDir) {
        activateTick--;
        List<VirtualEtherStream> snapshot = new ArrayList<>(streams);
        for (VirtualEtherStream ves : snapshot) {
            ves.tickCount++;

            if (ves.tickCount > Config.etherStreamMaxTick) {
                ves.markDead();
            }

            if (!ves.dead) {
                int consumption = ves.getConsumption();
                ves.consumeEther(consumption);
                if (ves.getEther() <= 0) {
                    ves.markDead();
                }
            }

            for (IStreamCapability cap : ves.capabilities) {
                cap.tick(ves);
            }
            if (ves.getEther() <= 0) {
                ves.markDead();
            }

            if (!ves.dead) {
                float motionLen = (float) ves.motion.length();
                ves.doCollision(cache, posDir, motionLen);
                ves.pos = ves.pos.add(ves.motion);
            }

            if (ves.dead && !ves.dying) {
                if (ves.label != null) {
                    ves.dying = true;
                    ves.deathTick = 0;
                    for (IStreamCapability cap : ves.capabilities) {
                        cap.onDestroy(ves);
                    }
                } else {
                    ves.deathTick = -1;
                }
            }
            if (ves.dying) {
                ves.deathTick++;
                ves.pos = ves.pos.add(ves.motion);
                if (ves.deathTick > 60) {
                    ves.deathTick = -1;
                }
            }
        }
        streams.removeIf(ves -> ves.deathTick == -1);
    }

    public boolean isDead() {
        return activateTick <= 0 && streams.isEmpty();
    }
}
