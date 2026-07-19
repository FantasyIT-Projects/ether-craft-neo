package studio.fantasyit.ether_craft.attachment;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LevelMuteSources {
    private final List<MuteRange> sources = new ArrayList<>();
    private final Map<BlockPos, Integer> sourceToId = new HashMap<>();
    private final Map<BlockPos, Long> sourceLastTickAt = new HashMap<>();
    private boolean shouldUpdate = false;

    public static StreamCodec<FriendlyByteBuf, LevelMuteSources> STREAM_CODEC_PARTIAL = StreamCodec.composite(
            MuteRange.STREAM_CODEC.apply(ByteBufCodecs.list()),
            LevelMuteSources::getSources,
            LevelMuteSources::new
    );

    public LevelMuteSources() {

    }

    public LevelMuteSources(List<MuteRange> sources) {
        this.sources.addAll(sources);
    }

    public List<MuteRange> getSources() {
        return sources;
    }

    public void tick(Level level) {
        long gt = level.getGameTime();
        for (int i = 0; i < sources.size(); ++i) {
            MuteRange mr = sources.get(i);
            if (Math.abs(sourceLastTickAt.getOrDefault(mr.pos(), 0L) - gt) > 10) {
                sources.remove(i);
                sourceToId.remove(mr.pos());
                sourceLastTickAt.remove(mr.pos());
                shouldUpdate = true;
                i--;
            }
        }

        if (shouldUpdate) {
            level.syncData(AttachmentDataRegistry.LEVEL_MUTE_SOURCE);
            shouldUpdate = false;
        }
    }

    public void notifyBlock(Level level, BlockPos pos, int rx, int ry, int rz) {
        sourceLastTickAt.put(pos, level.getGameTime());
        if (sourceToId.containsKey(pos)) {
            int idx = sourceToId.get(pos);
            MuteRange existing = sources.get(idx);
            if (existing.rx() != rx || existing.ry() != ry || existing.rz() != rz) {
                sources.set(idx, new MuteRange(pos, rx, ry, rz));
                shouldUpdate = true;
            }
            return;
        }
        int id = sources.size();
        sourceToId.put(pos, id);
        sources.add(new MuteRange(pos, rx, ry, rz));
        shouldUpdate = true;
    }

    public boolean checkMute(double x, double y, double z) {
        for (MuteRange mr : sources) {
            double dx = Math.abs(x - mr.pos().getX() - 0.5);
            double dy = Math.abs(y - mr.pos().getY() - 0.5);
            double dz = Math.abs(z - mr.pos().getZ() - 0.5);
            if (dx <= mr.rx() && dy <= mr.ry() && dz <= mr.rz()) {
                return true;
            }
        }
        return false;
    }
}
