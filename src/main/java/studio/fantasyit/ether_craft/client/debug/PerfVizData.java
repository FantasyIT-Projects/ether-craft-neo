package studio.fantasyit.ether_craft.client.debug;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import studio.fantasyit.ether_craft.network.c2s.PerfVizToggleC2S;
import studio.fantasyit.ether_craft.network.s2c.PerfVizSyncS2C;
import studio.fantasyit.ether_craft.perf.PerfTickData;
import studio.fantasyit.ether_craft.stream.PosDir;

public class PerfVizData {
    public record Stat(long lastNanos, long avgNanos, long maxNanos) {
        public static Stat of(PerfTickData.VeshSnapshot s) {
            return new Stat(s.lastNanos(), s.avgNanos(), s.maxNanos());
        }

        public static Stat of(PerfTickData.BlockSnapshot s) {
            return new Stat(s.lastNanos(), s.avgNanos(), s.maxNanos());
        }
    }

    private static boolean ENABLED = false;
    private static final Object2ObjectOpenHashMap<PosDir, Stat> veshStats = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<BlockPos, Stat> blockStats = new Object2ObjectOpenHashMap<>();

    private PerfVizData() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static void setEnabled(boolean enabled) {
        ENABLED = enabled;
        if (!enabled) {
            veshStats.clear();
            blockStats.clear();
        }
        ClientPacketDistributor.sendToServer(new PerfVizToggleC2S(enabled));
    }

    public static void handleSync(PerfVizSyncS2C msg) {
        if (!ENABLED) return;
        veshStats.clear();
        blockStats.clear();
        for (PerfTickData.VeshSnapshot s : msg.veshEntries()) {
            veshStats.put(s.posDir(), Stat.of(s));
        }
        for (PerfTickData.BlockSnapshot s : msg.blockEntries()) {
            blockStats.put(s.pos(), Stat.of(s));
        }
    }

    public static Object2ObjectOpenHashMap<PosDir, Stat> getVeshStats() {
        return veshStats;
    }

    public static Object2ObjectOpenHashMap<BlockPos, Stat> getBlockStats() {
        return blockStats;
    }
}
