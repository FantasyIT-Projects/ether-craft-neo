package studio.fantasyit.ether_craft.attachment;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LevelMuteSources {
    private final List<BlockPos> sources = new ArrayList<>();
    private final Map<BlockPos, Integer> sourceToId = new HashMap<>();
    private final Map<BlockPos, Long> sourceLastTickAt = new HashMap<>();
    private boolean shouldUpdate = false;

    public static StreamCodec<FriendlyByteBuf, LevelMuteSources> STREAM_CODEC_PARTIAL = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
            LevelMuteSources::getSources,
            LevelMuteSources::new
    );

    public LevelMuteSources() {

    }

    public LevelMuteSources(List<BlockPos> sources) {
        this.sources.addAll(sources);
    }

    public List<BlockPos> getSources() {
        return sources;
    }

    public void tick(Level level) {
        long gt = level.getGameTime();
        for (int i = 0; i < sources.size(); ++i) {
            BlockPos pos = sources.get(i);
            if (Math.abs(sourceLastTickAt.getOrDefault(pos, 0L) - gt) > 10) {
                sources.remove(i);
                sourceToId.remove(pos);
                sourceLastTickAt.remove(pos);
                shouldUpdate = true;
                i--;
            }
        }

        if (shouldUpdate) {
            level.syncData(AttachmentDataRegistry.LEVEL_MUTE_SOURCE);
            shouldUpdate = false;
        }
    }

    public void notifyBlock(Level level, BlockPos pos) {
        sourceLastTickAt.put(pos, level.getGameTime());
        if (sourceToId.containsKey(pos)) {
            return;
        }
        int id = sources.size();
        sourceToId.put(pos, id);
        sources.add(pos);
        shouldUpdate = true;
    }

    public boolean checkMute(double x, double y, double z) {
        for (BlockPos pos : sources) {
            if (pos.distToCenterSqr(x, y, z) <= Config.nodeMuteRange * Config.nodeMuteRange) {
                return true;
            }
        }
        return false;
    }
}
