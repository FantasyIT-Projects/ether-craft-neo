package studio.fantasyit.ether_craft.stream.client;


import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;
import studio.fantasyit.ether_craft.stream.EtherConsumer;

public class ClientStreamEntry {
    public Vec3 startPos = Vec3.ZERO;
    public Vec3 motion = Vec3.ZERO;
    public int startTickCount;

    public int tickCount;
    public int ether;
    public long receivedAtTick;
    public long deathAtTick;
    public int deathTick;
    public @Nullable Component label;
    public int labelColor;
    public boolean isDying;
    public boolean removed;
    public final EtherConsumer consumer = new EtherConsumer();

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
            this.label = entry.label();
            this.labelColor = entry.labelColor();
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
        ether = Math.max(0, ether - consumption);
    }

    public boolean isDying() {
        return isDying;
    }
}
