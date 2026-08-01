package studio.fantasyit.ether_craft.stream.idx;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.network.s2c.IndexMappingSyncS2C;
import studio.fantasyit.ether_craft.stream.PosDir;

import java.util.ArrayList;
import java.util.List;

public class IndexMappingManager {
    public Object2IntOpenHashMap<PosDir> pos2IdDir = new Object2IntOpenHashMap<>();
    public Object2IntOpenHashMap<PosDir> counter = new Object2IntOpenHashMap<>();
    public ArrayList<PosDir> toSyncPosDir = new ArrayList<>();
    int maxId = 0;


    public AutoIndexPosDir get(PosDir posDir) {
        if (pos2IdDir.containsKey(posDir)) {
            return new AutoIndexPosDir(pos2IdDir.get(posDir));
        }
        return new AutoIndexPosDir(posDir);
    }

    public void tick(ServerLevel level) {
        if (level.getServer().getTickCount() % Config.indexMappingDecayInterval == 0) {
            for (PosDir posDir : counter.keySet()) {
                counter.addTo(posDir, -1);
            }
            counter.object2IntEntrySet().removeIf(t -> t.getIntValue() <= 0);
        }

        if (!toSyncPosDir.isEmpty()) {
            List<IndexMappingSyncS2C.Entry> entries = new ArrayList<>();
            for (PosDir posDir : toSyncPosDir) {
                entries.add(new IndexMappingSyncS2C.Entry(pos2IdDir.get(posDir), posDir));
            }
            toSyncPosDir.clear();
            PacketDistributor.sendToPlayersInDimension(level, new IndexMappingSyncS2C(entries));
        }
    }

    public void syncToPlayer(ServerPlayer player) {
        List<IndexMappingSyncS2C.Entry> entries = new ArrayList<>();
        for (var entry : pos2IdDir.object2IntEntrySet()) {
            entries.add(new IndexMappingSyncS2C.Entry(entry.getIntValue(), entry.getKey()));
        }
        if (!entries.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new IndexMappingSyncS2C(entries));
        }
    }

    public void recordAndPrepareSend(PosDir pos) {
        if (pos2IdDir.containsKey(pos)) return;
        int i = counter.addTo(pos, 1);
        if (i >= Config.indexMappingRegisterThreshold) {
            counter.remove(pos, i);
            int id = maxId++;
            pos2IdDir.put(pos, id);
            toSyncPosDir.add(pos);
        }
    }
}
