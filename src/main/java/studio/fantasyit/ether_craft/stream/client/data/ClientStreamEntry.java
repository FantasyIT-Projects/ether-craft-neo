package studio.fantasyit.ether_craft.stream.client.data;


import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;
import studio.fantasyit.ether_craft.stream.EtherConsumer;
import studio.fantasyit.ether_craft.stream.EtherStreamConsumeModifier;
import studio.fantasyit.ether_craft.stream.client.extra.EtherStreamClientLogicManager;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;

import java.util.HashMap;
import java.util.Map;

public class ClientStreamEntry {
    public Vec3 startPos = Vec3.ZERO;
    public Vec3 motion = Vec3.ZERO;
    public int startTickCount;

    public int tickCount;
    public int ether;
    public long receivedAtTick;
    public long deathAtTick;
    public int deathTick;
    public boolean isDying;
    public boolean removed;
    public final EtherConsumer consumer = new EtherConsumer();
    public Map<Identifier, IEtherStreamSyncedData> syncedData = new HashMap<>();

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
            this.startPos = entry.startPos();
            this.motion = entry.motion();
            this.startTickCount = entry.tickCount();
            this.tickCount = entry.tickCount();
            this.ether = entry.ether();
            this.consumer.fromState(entry.consumerState());
            this.syncedData = new HashMap<>();
            for (IEtherStreamSyncedData data : entry.syncedData())
                this.syncedData.put(data.getId(), data);
        }
        Level level = Minecraft.getInstance().level;
        this.receivedAtTick = level != null ? level.getGameTime() : 0;
    }

    public void updateFromServer(int ether, EtherConsumer.State consumerState) {
        this.ether = ether;
        this.consumer.fromState(consumerState);
    }

    public void tick() {
        if (isDying) {
            deathTick--;
            if (deathTick <= 0) {
                removed = true;
                isDying = false;
            }
            return;
        }

        tickCount++;
        int consumption = consumer.getTotalConsumption(ether, tickCount);
        Vec3 position = startPos.add(motion.scale(tickCount - startTickCount));
        consumption = EtherStreamConsumeModifier.modify(consumption, ether, tickCount, Minecraft.getInstance().level, position);
        ether = Math.max(0, ether - consumption);
        EtherStreamClientLogicManager.onTick(this);
    }

    public boolean isDying() {
        return isDying;
    }

    public Vec3 getCurrentPosition() {
        return startPos.add(motion.scale(tickCount - startTickCount));
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
}
