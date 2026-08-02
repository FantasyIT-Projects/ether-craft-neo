package studio.fantasyit.ether_craft.perf;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.stream.PosDir;

import java.util.ArrayList;
import java.util.List;

public class PerfTickData {
    public static final int STALE_TICKS = 60;

    private final Object2ObjectOpenHashMap<PosDir, Entry> veshStats = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<BlockPos, Entry> blockStats = new Object2ObjectOpenHashMap<>();

    private static class Entry {
        final TickStat stat = new TickStat();
        long lastSeenTick;
    }

    public static PerfTickData get(Level level) {
        return level.getData(AttachmentDataRegistry.PERF_TICK_DATA);
    }

    public void recordVESH(PosDir posDir, long nanos, long gameTime) {
        Entry entry = veshStats.computeIfAbsent(posDir, k -> new Entry());
        entry.lastSeenTick = gameTime;
        entry.stat.record(nanos);
    }

    public void recordBlock(BlockPos pos, long nanos, long gameTime) {
        Entry entry = blockStats.computeIfAbsent(pos, k -> new Entry());
        entry.lastSeenTick = gameTime;
        entry.stat.record(nanos);
    }

    public void purge(long nowGameTime) {
        veshStats.values().removeIf(e -> nowGameTime - e.lastSeenTick > STALE_TICKS);
        blockStats.values().removeIf(e -> nowGameTime - e.lastSeenTick > STALE_TICKS);
    }

    public List<VeshSnapshot> veshSnapshots() {
        List<VeshSnapshot> list = new ArrayList<>(veshStats.size());
        for (var e : veshStats.object2ObjectEntrySet()) {
            Entry entry = e.getValue();
            list.add(new VeshSnapshot(e.getKey(), entry.stat.lastNanos(), entry.stat.avgNanos(), entry.stat.maxNanos()));
        }
        return list;
    }

    public List<BlockSnapshot> blockSnapshots() {
        List<BlockSnapshot> list = new ArrayList<>(blockStats.size());
        for (var e : blockStats.object2ObjectEntrySet()) {
            Entry entry = e.getValue();
            list.add(new BlockSnapshot(e.getKey(), entry.stat.lastNanos(), entry.stat.avgNanos(), entry.stat.maxNanos()));
        }
        return list;
    }

    public record VeshSnapshot(PosDir posDir, long lastNanos, long avgNanos, long maxNanos) {
        public static final StreamCodec<RegistryFriendlyByteBuf, VeshSnapshot> STREAM_CODEC = StreamCodec.composite(
                PosDir.STREAM_CODEC,
                VeshSnapshot::posDir,
                ByteBufCodecs.VAR_LONG,
                VeshSnapshot::lastNanos,
                ByteBufCodecs.VAR_LONG,
                VeshSnapshot::avgNanos,
                ByteBufCodecs.VAR_LONG,
                VeshSnapshot::maxNanos,
                VeshSnapshot::new
        );
    }

    public record BlockSnapshot(BlockPos pos, long lastNanos, long avgNanos, long maxNanos) {
        public static final StreamCodec<RegistryFriendlyByteBuf, BlockSnapshot> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                BlockSnapshot::pos,
                ByteBufCodecs.VAR_LONG,
                BlockSnapshot::lastNanos,
                ByteBufCodecs.VAR_LONG,
                BlockSnapshot::avgNanos,
                ByteBufCodecs.VAR_LONG,
                BlockSnapshot::maxNanos,
                BlockSnapshot::new
        );
    }
}
