package studio.fantasyit.ether_craft.stream.vholder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.PosDir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualEtherStreamHolderManager {
    private final Map<PosDir, VirtualEtherStreamHolder> holders = new HashMap<>();
    private boolean levelInitialized = false;

    private record VESHMapEntry(PosDir posDir, VirtualEtherStreamHolderData holderData) {
        static final Codec<VESHMapEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PosDir.CODEC.fieldOf("posDir").forGetter(VESHMapEntry::posDir),
                VirtualEtherStreamHolderData.CODEC.fieldOf("holder").forGetter(VESHMapEntry::holderData)
        ).apply(instance, VESHMapEntry::new));
    }

    public static final Codec<VirtualEtherStreamHolderManager> CODEC = VESHMapEntry.CODEC.listOf().xmap(
            entries -> {
                VirtualEtherStreamHolderManager mgr = new VirtualEtherStreamHolderManager();
                for (VESHMapEntry entry : entries) {
                    PosDir posDir = entry.posDir();
                    VirtualEtherStreamHolder holder = new VirtualEtherStreamHolder(posDir.pos(), posDir.dir(), null);
                    VirtualEtherStreamHolderData data = entry.holderData();
                    holder.activateTick = data.activateTick();
                    holder.nextId = data.nextId();
                    for (VirtualEtherStreamData streamData : data.streams()) {
                        VirtualEtherStream ves = VirtualEtherStream.fromData(null, streamData);
                        holder.streams.add(ves);
                    }
                    mgr.holders.put(posDir, holder);
                }
                return mgr;
            },
            mgr -> {
                List<VESHMapEntry> entries = new ArrayList<>();
                for (Map.Entry<PosDir, VirtualEtherStreamHolder> e : mgr.holders.entrySet()) {
                    entries.add(new VESHMapEntry(e.getKey(), e.getValue().toData()));
                }
                return entries;
            }
    );

    public VirtualEtherStreamHolderManager() {}

    public IEtherStreamLike createStream(Level level, PosDir posDir, int ether, Vec3 pos, Vec3 motion) {
        VirtualEtherStreamHolder holder = holders.computeIfAbsent(posDir,
                k -> new VirtualEtherStreamHolder(posDir.pos(), posDir.dir(), (ServerLevel) level));
        holder.activateTick = 5;
        VirtualEtherStream ves = holder.createStream(ether, pos, motion);
        ves.level = level;

        return ves;
    }

    public boolean canCreateStream(PosDir posDir) {
        VirtualEtherStreamHolder holder = holders.get(posDir);
        if (holder == null) return true;
        return !holder.hasStreamInUnloadedChunk();
    }

    public void tick(ServerLevel level) {
        initLevel(level);

        List<PosDir> toRemove = new ArrayList<>();
        for (Map.Entry<PosDir, VirtualEtherStreamHolder> entry : holders.entrySet()) {
            PosDir posDir = entry.getKey();
            VirtualEtherStreamHolder holder = entry.getValue();
            holder.tick(posDir);
            if (holder.isDead()) {
                toRemove.add(posDir);
            }
        }
        for (PosDir posDir : toRemove) {
            holders.remove(posDir);
        }
    }

    private void initLevel(ServerLevel level) {
        if (levelInitialized) return;
        levelInitialized = true;
        for (VirtualEtherStreamHolder holder : holders.values()) {
            holder.initLevel(level);
        }
    }

    public static VirtualEtherStreamHolderManager get(ServerLevel level) {
        VirtualEtherStreamHolderManager mgr = level.getData(studio.fantasyit.ether_craft.register.AttachmentDataRegistry.VESHM);
        mgr.initLevel(level);
        return mgr;
    }
}
