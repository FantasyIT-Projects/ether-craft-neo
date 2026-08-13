package studio.fantasyit.ether_craft.stream.vholder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.entity.EntitySectionCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualEtherStreamHolderManager {
    public record VESHEntry(PosDir posDir, VirtualEtherStreamHolderData holderData) {
        static final Codec<VESHEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PosDir.CODEC.fieldOf("pos_dir").forGetter(VESHEntry::posDir),
                VirtualEtherStreamHolderData.CODEC.fieldOf("holder").forGetter(VESHEntry::holderData)
        ).apply(instance, VESHEntry::new));
    }

    public static final Codec<VirtualEtherStreamHolderManager> CODEC = VESHEntry.CODEC.listOf().xmap(VirtualEtherStreamHolderManager::new, VirtualEtherStreamHolderManager::toData);


    private final Map<PosDir, VirtualEtherStreamHolder> holders = new HashMap<>();
    private final List<VirtualEtherStreamHolder> holderList = new ArrayList<>();
    private List<VESHEntry> lazyLoadData;
    public final EntitySectionCache sectorCache = new EntitySectionCache();
    private int currentMaxHolderId = 0;
    private int tickCount = 0;
    public int simulateInterval;

    public static VirtualEtherStreamHolderManager empty() {
        return new VirtualEtherStreamHolderManager(null);
    }

    public VirtualEtherStreamHolderManager(@Nullable List<VESHEntry> toBeLoaded) {
        lazyLoadData = toBeLoaded;
        simulateInterval = Config.etherStreamSimulateInterval;
    }

    private void ensureLazy(ServerLevel level) {
        if (lazyLoadData != null) {
            for (VESHEntry entry : lazyLoadData) {
                VirtualEtherStreamHolder virtualEtherStreamHolder = createHolder(level, entry.posDir);
                virtualEtherStreamHolder.loadFromData(entry.holderData);
                holders.put(entry.posDir, virtualEtherStreamHolder);
                holderList.add(virtualEtherStreamHolder);
            }
            lazyLoadData = null;
        }
    }

    public VirtualEtherStreamHolder getHolderOrCreate(ServerLevel level, PosDir posDir) {
        ensureLazy(level);
        if (holders.containsKey(posDir))
            return holders.get(posDir);
        VirtualEtherStreamHolder virtualEtherStreamHolder = createHolder(level, posDir);
        holders.put(posDir, virtualEtherStreamHolder);
        holderList.add(virtualEtherStreamHolder);
        return holders.get(posDir);
    }

    public IEtherStreamLike createStream(Level level, PosDir posDir, int ether, float offset, float speed) {
        VirtualEtherStreamHolder holder = this.getHolderOrCreate((ServerLevel) level, posDir);
        return holder.createStream(ether, offset, speed, tickCount);
    }

    public boolean canCreateStream(PosDir posDir) {
        VirtualEtherStreamHolder holder = holders.get(posDir);
        if (holder == null) return true;
        return !holder.isStreamBlocked();
    }

    public void tick(ServerLevel level) {
        sectorCache.clear();
        ensureLazy(level);
        PlayerPayloadAccumulator acc = new PlayerPayloadAccumulator();
        tickCount++;
        for (int i = 0; i < holderList.size(); i++) {
            VirtualEtherStreamHolder holder = holderList.get(i);
            if (!holder.shouldTick(tickCount)) continue;
            PosDir posDir = holder.posDir;
            holder.tick(acc);
            if (holder.isDead()) {
                holders.remove(posDir);
                holderList.remove(i);
                i--;
            }
        }
        acc.send(level);
    }

    public void syncAndStratTrackingByPlayer(ServerPlayer player) {
        for (Map.Entry<PosDir, VirtualEtherStreamHolder> entry : holders.entrySet()) {
            entry.getValue().syncAndStartTrackingByPlayer(player);
        }
    }

    public void clearQuickState(int playerId) {
        for (Map.Entry<PosDir, VirtualEtherStreamHolder> entry : holders.entrySet()) {
            entry.getValue().clearPlayerQuickState(playerId);
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

    private VirtualEtherStreamHolder createHolder(ServerLevel level, PosDir posDir) {
        return new VirtualEtherStreamHolder(posDir, this, level, simulateInterval, currentMaxHolderId++);
    }
}
