package studio.fantasyit.ether_craft.entity.vholder;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamUpdateS2C;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VESHM {
    private final Map<PosDir, VirtualEtherStreamHolder> holders = new HashMap<>();
    private final ChainedEmitterEntityHitCache cache = new ChainedEmitterEntityHitCache();

    public VESHM() {}

    public IEtherStreamLike createStream(Level level, PosDir posDir, int ether, Vec3 pos, Vec3 motion) {
        VirtualEtherStreamHolder holder = holders.computeIfAbsent(posDir,
                k -> new VirtualEtherStreamHolder(posDir.dir()));
        holder.activateTick = 5;
        VirtualEtherStream ves = holder.createStream(ether, pos, motion);
        ves.level = level;

        if (level instanceof ServerLevel serverLevel) {
            var payload = new EtherStreamCreateS2C(posDir, ves.streamId,
                    ves.startPos, ves.motion, ves.ether, ves.tickCount,
                    ves.label, ves.labelColor);
            PacketDistributor.sendToPlayersTrackingChunk(
                    serverLevel, serverLevel.getChunk(posDir.pos()).getPos(), payload);
        }

        return ves;
    }

    public void tick(ServerLevel level) {
        cache.beforeTick();
        List<PosDir> toRemove = new ArrayList<>();
        for (Map.Entry<PosDir, VirtualEtherStreamHolder> entry : holders.entrySet()) {
            PosDir posDir = entry.getKey();
            VirtualEtherStreamHolder holder = entry.getValue();
            holder.tick(cache, posDir);
            if (holder.isDead()) {
                toRemove.add(posDir);
            }
        }
        for (PosDir posDir : toRemove) {
            holders.remove(posDir);
        }

        // send sync payloads to tracking clients
        for (var entry : holders.entrySet()) {
            PosDir posDir = entry.getKey();
            VirtualEtherStreamHolder holder = entry.getValue();

            List<EtherStreamUpdateS2C.StreamEntry> streamEntries = new ArrayList<>();
            for (VirtualEtherStream ves : holder.streams) {
                byte flags = 0;
                if (ves.dead) flags |= EtherStreamUpdateS2C.StreamEntry.FLAG_DEAD;
                if (ves.dying) flags |= EtherStreamUpdateS2C.StreamEntry.FLAG_DYING;
                streamEntries.add(new EtherStreamUpdateS2C.StreamEntry(
                        ves.streamId, ves.tickCount, ves.ether, flags,
                        ves.deathTick, ves.label, ves.labelColor
                ));
            }

            if (!streamEntries.isEmpty() || !holder.streams.isEmpty()) {
                var payload = new EtherStreamUpdateS2C(posDir, streamEntries);
                PacketDistributor.sendToPlayersTrackingChunk(
                        level, level.getChunk(posDir.pos()).getPos(), payload);
            }
        }
    }

    public static VESHM get(ServerLevel level) {
        return level.getData(studio.fantasyit.ether_craft.register.AttachmentDataRegistry.VESHM);
    }
}
