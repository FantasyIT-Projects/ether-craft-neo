package studio.fantasyit.ether_craft.stream.client.data;

import studio.fantasyit.ether_craft.entity.stream.EtherStreamEntity;
import studio.fantasyit.ether_craft.stream.client.extra.IEtherStreamExtraClientLogic;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EntityStreamClientManager {
    public static final List<ClientStreamEntry> entries = new ArrayList<>();
    public static final List<ClientStreamEntry> dyings = new ArrayList<>();
    public static final HashMap<Integer, Integer> noTickCount = new HashMap<>();

    public static void tick() {
        for (var entry : entries) {
            int noTick = noTickCount.getOrDefault(entry.id, 0) + 1;
            noTickCount.put(entry.id, noTick);
            if (noTick > 1 && !entry.isDying && !entry.removed) {
                markDead(entry);
            }
        }
        entries.removeIf(entry -> entry.removed || entry.isDying);
        noTickCount.entrySet().removeIf(e -> e.getValue() > 1);

        for (var entry : entries) {
            for (var logic : entry.attachedLogic) {
                logic.onTick(entry);
            }
        }

        for (var entry : dyings) {
            entry.deathTick--;
            if (entry.deathTick <= 0) {
                entry.removed = true;
                entry.attachedLogic.forEach(l -> l.onDestroy(entry));
            }
        }
        dyings.removeIf(e -> e.removed);
    }

    private static void markDead(ClientStreamEntry entry) {
        entry.updateDynamic();
        if (entry.attachedLogic.stream().anyMatch(l -> l.shouldDelayDeath(entry))) {
            entry.setDying();
            dyings.add(entry);
        } else {
            entry.attachedLogic.forEach(l -> l.onDestroy(entry));
            entry.setRemoved();
        }
    }

    private static ClientStreamEntry get(EtherStreamEntity entity) {
        for (var entry : entries) {
            if (entry.id == entity.getId())
                return entry;
        }
        return null;
    }

    private static ClientStreamEntry getOrCreate(EtherStreamEntity entity) {
        for (var entry : entries) {
            if (entry.id == entity.getId())
                return entry;
        }
        ClientStreamEntry entry = ClientStreamEntry.fromEntity(entity);
        entries.add(entry);
        return entry;
    }

    private static void syncFromEntity(ClientStreamEntry entry, EtherStreamEntity entity) {
        entry.currentPos = entity.position();
        entry.motion = entity.getDeltaMovement();
        entry.ether = entity.getEther();
        entry.consumer.fromState(entity.consumer.toState());

        List<IEtherStreamSyncedData> synced = entity.getEntityData().get(EtherStreamEntity.SYNCED_DATA);
        entry.syncedData.clear();
        for (IEtherStreamSyncedData data : synced) {
            entry.syncedData.put(data.getId(), data);
        }
        entry.updateDynamic();
    }

    public static void tickEntity(EtherStreamEntity entity) {
        ClientStreamEntry entry = getOrCreate(entity);
        syncFromEntity(entry, entity);
        noTickCount.put(entry.id, 0);
        boolean shouldRender = true;
        for (IEtherStreamExtraClientLogic logic : entry.attachedLogic)
            if (!logic.shouldRender(entry))
                shouldRender = false;
        entity.setInvisible(!shouldRender);
    }

    public static void markDead(EtherStreamEntity etherStreamEntity) {
        ClientStreamEntry entry = get(etherStreamEntity);
        if (entry == null)
            return;
        markDead(entry);
    }
}
