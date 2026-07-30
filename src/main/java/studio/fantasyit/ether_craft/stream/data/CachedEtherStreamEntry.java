package studio.fantasyit.ether_craft.stream.data;

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

    public CachedEtherStreamEntry withId(int newId) {
        return new CachedEtherStreamEntry(
                newId, startOffset, startSpeed, ether, tickCount, consumerState
        );
    }

    @Override
    public List<IEtherStreamSyncedData> syncedData() {
        return List.of();
    }
}
