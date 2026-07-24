package studio.fantasyit.ether_craft.stream.vholder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.PosDir;

import java.util.*;

public class VirtualEtherStreamHolderManager {
    public record VESHEntry(PosDir posDir, VirtualEtherStreamHolderData holderData) {
        static final Codec<VESHEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PosDir.CODEC.fieldOf("pos_dir").forGetter(VESHEntry::posDir),
                VirtualEtherStreamHolderData.CODEC.fieldOf("holder").forGetter(VESHEntry::holderData)
        ).apply(instance, VESHEntry::new));
    }

    public static final Codec<VirtualEtherStreamHolderManager> CODEC = VESHEntry.CODEC.listOf().xmap(VirtualEtherStreamHolderManager::new, VirtualEtherStreamHolderManager::toData);


    private final Map<PosDir, VirtualEtherStreamHolder> holders = new HashMap<>();
    private List<VESHEntry> lazyLoadData;

    public static VirtualEtherStreamHolderManager empty() {
        return new VirtualEtherStreamHolderManager(null);
    }

    public VirtualEtherStreamHolderManager(@Nullable List<VESHEntry> toBeLoaded) {
        lazyLoadData = toBeLoaded;
    }

    private void ensureLazy(ServerLevel level) {
        if (lazyLoadData != null) {
            for (VESHEntry entry : lazyLoadData) {
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

    public IEtherStreamLike createStream(Level level, PosDir posDir, int ether, float offset, float speed) {
        VirtualEtherStreamHolder holder = this.getHolderOrCreate((ServerLevel) level, posDir);
        return holder.createStream(ether, offset, speed);
    }

    public boolean canCreateStream(PosDir posDir) {
        VirtualEtherStreamHolder holder = holders.get(posDir);
        if (holder == null) return true;
        return !holder.isStreamBlocked();
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

    public void syncAndStratTrackingByPlayer(ServerPlayer player) {
        for (Map.Entry<PosDir, VirtualEtherStreamHolder> entry : holders.entrySet()) {
            entry.getValue().syncAndStartTrackingByPlayer(player);
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

    public List<VESHEntry> toData() {
        List<VESHEntry> entries = new ArrayList<>();
        for (Map.Entry<PosDir, VirtualEtherStreamHolder> e : holders.entrySet()) {
            entries.add(new VESHEntry(e.getKey(), e.getValue().toData()));
        }
        return entries;
    }
}
