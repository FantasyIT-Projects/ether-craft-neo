package studio.fantasyit.ether_craft.stream.data;

import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.stream.EtherConsumer;

import java.util.List;

public record CachedEtherStreamEntry(
        int streamId,
        float startOffset,
        float startSpeed,
        int ether,
        int tickCount,
        EtherConsumer.State consumerState
) implements IEtherStreamEntryLike {

    public static CachedEtherStreamEntry from(IEtherStreamEntryLike source) {
        return new CachedEtherStreamEntry(
                source.streamId(),
                source.startOffset(),
                source.startSpeed(),
                source.ether(),
                source.tickCount(),
                source.consumerState()
        );
    }

    /** 派生只改 ID、可按需覆盖 tickCount/ether 的副本；null 表示沿用当前值 */
    public CachedEtherStreamEntry withNext(int newId, @Nullable Integer tickCount, @Nullable Integer ether) {
        return new CachedEtherStreamEntry(
                newId, startOffset, startSpeed,
                ether != null ? ether : this.ether,
                tickCount != null ? tickCount : this.tickCount,
                consumerState
        );
    }

    @Override
    public List<IEtherStreamSyncedData> syncedData() {
        return List.of();
    }
}
