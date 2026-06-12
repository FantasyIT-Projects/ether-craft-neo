package studio.fantasyit.ether_craft.stream.client.data;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import studio.fantasyit.ether_craft.entity.stream.EtherStreamEntity;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntityStreamClientManager {
    public static final List<ClientStreamEntry> entries = new ArrayList<>();

    public static void tick(Level level) {
        Set<Integer> seen = new HashSet<>();
        List<EtherStreamEntity> entities = level.getEntities(
                EntityTypeTest.forClass(EtherStreamEntity.class),
                new AABB(-30000000, -256, -30000000, 30000000, 512, 30000000),
                e -> true
        );

        for (EtherStreamEntity entity : entities) {
            seen.add(entity.getId());
            ClientStreamEntry entry = getOrCreate(entity);
            syncFromEntity(entry, entity);
        }

        for (var entry : entries) {
            if (!seen.contains(entry.id) && !entry.isDying && !entry.removed) {
                entry.updateDynamic();
                if (entry.attachedLogic.stream().anyMatch(l -> l.shouldDelayDeath(entry))) {
                    entry.setDying();
                } else {
                    entry.setRemoved();
                }
            }
        }

        for (var entry : entries) {
            if (entry.isDying) {
                entry.deathTick--;
                if (entry.deathTick <= 0) {
                    entry.removed = true;
                    entry.isDying = false;
                }
            }
            for (var logic : entry.attachedLogic) {
                logic.onTick(entry);
            }
        }

        entries.removeIf(e -> {
            if (e.removed) {
                e.attachedLogic.forEach(l -> l.onDestroy(e));
                return true;
            }
            return false;
        });
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
        if (synced != null) {
            entry.syncedData.clear();
            for (IEtherStreamSyncedData data : synced) {
                entry.syncedData.put(data.getId(), data);
            }
        }

        if (entity.getEntityData().get(EtherStreamEntity.DYING)) {
            entry.setDying();
        }

        entry.updateDynamic();
    }

    public static ClientStreamEntry getEntry(int entityId) {
        for (var entry : entries) {
            if (entry.id == entityId)
                return entry;
        }
        return null;
    }
}
