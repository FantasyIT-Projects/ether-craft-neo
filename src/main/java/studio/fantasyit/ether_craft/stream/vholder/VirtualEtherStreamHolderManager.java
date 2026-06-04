package studio.fantasyit.ether_craft.stream.vholder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.PosDir;

import java.util.*;

public class VirtualEtherStreamHolderManager {
    private record VESHMapEntry(PosDir posDir, VirtualEtherStreamHolderData holderData) {
        static final Codec<VESHMapEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PosDir.CODEC.fieldOf("pos_dir").forGetter(VESHMapEntry::posDir),
                VirtualEtherStreamHolderData.CODEC.fieldOf("holder").forGetter(VESHMapEntry::holderData)
        ).apply(instance, VESHMapEntry::new));
    }

    public static final Codec<VirtualEtherStreamHolderManager> CODEC = VESHMapEntry.CODEC.listOf().xmap(VirtualEtherStreamHolderManager::new,
            mgr -> {
                List<VESHMapEntry> entries = new ArrayList<>();
                for (Map.Entry<PosDir, VirtualEtherStreamHolder> e : mgr.holders.entrySet()) {
                    entries.add(new VESHMapEntry(e.getKey(), e.getValue().toData()));
                }
                return entries;
            }
    );


    private final Map<PosDir, VirtualEtherStreamHolder> holders = new HashMap<>();
    private List<VESHMapEntry> lazyLoadData;

    public VirtualEtherStreamHolderManager(@Nullable List<VESHMapEntry> toBeLoaded) {
        lazyLoadData = toBeLoaded;
    }

    private void ensureLazy(ServerLevel level) {
        if (lazyLoadData != null) {
            for (VESHMapEntry entry : lazyLoadData) {
                VirtualEtherStreamHolder virtualEtherStreamHolder = new VirtualEtherStreamHolder(entry.posDir, level);
                virtualEtherStreamHolder.loadFromData(entry.holderData);
                holders.put(entry.posDir, virtualEtherStreamHolder);
            }
            lazyLoadData = null;
        }
    }

    public VirtualEtherStreamHolder getHolderOrCreate(ServerLevel level, PosDir posDir) {
        ensureLazy(level);
        if (holders.containsKey(posDir))
            return holders.get(posDir);
        holders.put(posDir, new VirtualEtherStreamHolder(posDir, level));
        return holders.get(posDir);
    }

    public IEtherStreamLike createStream(Level level, PosDir posDir, int ether, Vec3 pos, Vec3 motion) {
        VirtualEtherStreamHolder holder = this.getHolderOrCreate((ServerLevel) level, posDir);
        return holder.createStream(ether, pos, motion);
    }

    public boolean canCreateStream(PosDir posDir) {
        //如果有任何VES到达了未加载区块，那么整个VESH应该停止继续生成新的以太粒子
        VirtualEtherStreamHolder holder = holders.get(posDir);
        if (holder == null) return true;
        return !holder.hasStreamInUnloadedChunk();
    }

    public void tick(ServerLevel level) {
        ensureLazy(level);
        HashSet<Map.Entry<PosDir, VirtualEtherStreamHolder>> keys = new HashSet<>(holders.entrySet());
        for (Map.Entry<PosDir, VirtualEtherStreamHolder> entry : keys) {
            PosDir posDir = entry.getKey();
            VirtualEtherStreamHolder holder = entry.getValue();
            holder.tick();
            if (holder.isDead()) {
                holders.remove(posDir);
            }
        }
    }

    public void syncAllToPlayer(ServerPlayer player) {
        for (Map.Entry<PosDir, VirtualEtherStreamHolder> entry : holders.entrySet()) {
            entry.getValue().syncToPlayer(player);
        }
    }

    public static VirtualEtherStreamHolderManager get(ServerLevel level) {
        VirtualEtherStreamHolderManager mgr = level.getData(AttachmentDataRegistry.VESHM);
        mgr.ensureLazy(level);
        return mgr;
    }

    public VirtualEtherStreamHolder getHolder(PosDir posDir) {
        return holders.get(posDir);
    }
}
