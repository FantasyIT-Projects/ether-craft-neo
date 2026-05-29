package studio.fantasyit.ether_craft.stream.vholder;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualEtherStreamHolderManager {
    private final Map<PosDir, VirtualEtherStreamHolder> holders = new HashMap<>();
    private final ChainedEmitterEntityHitCache cache = new ChainedEmitterEntityHitCache();

    public VirtualEtherStreamHolderManager() {}

    public IEtherStreamLike createStream(Level level, PosDir posDir, int ether, Vec3 pos, Vec3 motion) {
        VirtualEtherStreamHolder holder = holders.computeIfAbsent(posDir,
                k -> new VirtualEtherStreamHolder(posDir.pos(), posDir.dir(), (ServerLevel) level));
        holder.activateTick = 5;
        VirtualEtherStream ves = holder.createStream(ether, pos, motion);
        ves.level = level;

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
    }

    public static VirtualEtherStreamHolderManager get(ServerLevel level) {
        return level.getData(studio.fantasyit.ether_craft.register.AttachmentDataRegistry.VESHM);
    }
}
