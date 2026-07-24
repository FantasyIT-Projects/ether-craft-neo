package studio.fantasyit.ether_craft.stream.data;

import studio.fantasyit.ether_craft.stream.EtherConsumer;

import java.util.List;

public interface IEtherStreamEntryLike {
    int streamId();
    float startOffset();
    float startSpeed();
    int ether();
    int tickCount();
    EtherConsumer.State consumerState();
    List<IEtherStreamSyncedData> syncedData();
}
