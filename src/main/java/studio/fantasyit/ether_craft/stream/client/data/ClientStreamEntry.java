package studio.fantasyit.ether_craft.stream.client.data;


import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.entity.stream.EtherStreamEntity;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;
import studio.fantasyit.ether_craft.stream.EtherConsumer;
import studio.fantasyit.ether_craft.stream.client.extra.EtherStreamClientLogicManager;
import studio.fantasyit.ether_craft.stream.client.extra.IEtherStreamExtraClientLogic;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClientStreamEntry {
    public Vec3 startPos = Vec3.ZERO;
    public Vec3 motion = Vec3.ZERO;
    public Vec3 currentPos = Vec3.ZERO;
    public Vec3[] reverseStepMotions = new Vec3[6];
    public int startTickCount;

    public int id;

    public int tickCount;
    public int ether;
    public long receivedAtTick;
    public long deathAtTick;
    public int deathTick;
    public boolean isDying;
    public boolean removed;
    public int noEtherTicks = 0;
    public boolean shouldRender = true;
    public final EtherConsumer consumer = new EtherConsumer();
    public Map<Identifier, IEtherStreamSyncedData> syncedData = new Object2ObjectOpenHashMap<>();

    public List<IEtherStreamExtraClientLogic> attachedLogic = new ArrayList<>();

    public void setDying() {
        if (!this.isDying) {
            this.deathTick = 60;
        }
        this.isDying = true;
    }

    public void setRemoved() {
        this.removed = true;
    }

    public boolean isRemoved() {
        return removed;
    }

    public ClientStreamEntry(@Nullable EtherStreamCreateS2C.StreamEntry entry) {
        if (entry != null) {
            this.id = entry.streamId();
            this.startPos = entry.startPos();
            this.motion = entry.motion();
            this.startTickCount = entry.tickCount();
            this.tickCount = entry.tickCount();
            this.currentPos = entry.startPos().add(entry.motion().scale(entry.tickCount()));
            this.ether = entry.ether();
            this.consumer.fromState(entry.consumerState());
            this.syncedData = new Object2ObjectOpenHashMap<>();
            for (IEtherStreamSyncedData data : entry.syncedData())
                this.syncedData.put(data.getId(), data);
            for (int i = 0; i < reverseStepMotions.length; i++) {
                reverseStepMotions[i] = motion.reverse().scale(i);
            }
        } else {
            this.id = -1;
        }
        Level level = Minecraft.getInstance().level;
        this.receivedAtTick = level != null ? level.getGameTime() : 0;
        updateDynamic();
    }

    public static ClientStreamEntry fromEntity(EtherStreamEntity entity) {
        ClientStreamEntry entry = new ClientStreamEntry((EtherStreamCreateS2C.StreamEntry) null);
        entry.id = entity.getId();
        entry.startPos = entity.position();
        entry.motion = entity.getDeltaMovement();
        entry.currentPos = entity.position();
        entry.ether = entity.getEther();
        entry.consumer.fromState(entity.consumer.toState());
        entry.syncedData = new Object2ObjectOpenHashMap<>();
        List<IEtherStreamSyncedData> synced = entity.getEntityData().get(EtherStreamEntity.SYNCED_DATA);
        if (synced != null) {
            for (IEtherStreamSyncedData data : synced) {
                entry.syncedData.put(data.getId(), data);
            }
        }
        for (int i = 0; i < entry.reverseStepMotions.length; i++) {
            entry.reverseStepMotions[i] = entry.motion.reverse().scale(i);
        }
        Level level = Minecraft.getInstance().level;
        entry.receivedAtTick = level != null ? level.getGameTime() : 0;
        entry.updateDynamic();
        return entry;
    }

    public void updateFromServer(int ether, EtherConsumer.State consumerState) {
        this.ether = ether;
        this.consumer.fromState(consumerState);
    }

    public void tick(Level level) {
        if (isDying) {
            deathTick--;
            if (deathTick <= 0) {
                removed = true;
                isDying = false;
            }
            return;
        }

        tickCount++;
        currentPos = currentPos.add(motion);
        int consumption = consumer.getTotalConsumption(ether, tickCount);
        ether = Math.max(0, ether - consumption);
        for (IEtherStreamExtraClientLogic logic : attachedLogic)
            logic.onTick(this);
        if (ether <= 0) {
            noEtherTicks++;
            if (noEtherTicks >= 60) {
                removed = true;
            }
        } else {
            noEtherTicks = 0;
        }
    }

    public boolean isDying() {
        return isDying;
    }

    public Vec3 getCurrentPosition() {
        return currentPos;
    }

    public @Nullable IEtherStreamSyncedData getSyncedData(Identifier id) {
        return syncedData.get(id);
    }

    public void setSyncedData(IEtherStreamSyncedData data) {
        syncedData.put(data.getId(), data);
    }

    public void removeSyncedData(Identifier id) {
        syncedData.remove(id);
    }

    public void updateDynamic() {
        EtherStreamClientLogicManager.reApplyAttach(this);
        shouldRender = attachedLogic.stream().allMatch(t -> t.shouldRender(this));
    }
}
