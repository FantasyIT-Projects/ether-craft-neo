package studio.fantasyit.ether_craft.stream.client;


import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;

public class ClientStreamEntry {
    public Vec3 startPos = Vec3.ZERO;
    public Vec3 motion = Vec3.ZERO;
    public int startTickCount;
    public int ether;
    public long receivedAtTick;
    public int deathTick;
    public @Nullable Component label;
    public int labelColor;
    public boolean isDying;
    public boolean removed;

    public void setDying(boolean dying) {
        this.isDying = dying;
        if (dying) {
            this.deathTick = 60;
        }
    }

    public boolean isRemoved() {
        return removed;
    }

    public ClientStreamEntry(@Nullable EtherStreamCreateS2C msg) {
        if (msg != null) {
            this.startPos = msg.startPos();
            this.motion = msg.motion();
            this.startTickCount = msg.tickCount();
            this.ether = msg.ether();
            this.label = msg.label();
            this.labelColor = msg.labelColor();
        }
        Level level = Minecraft.getInstance().level;
        this.receivedAtTick = level != null ? level.getGameTime() : 0;
    }

    public void tick() {
        if (isDying) {
            deathTick--;
            if (deathTick <= 0) {
                removed = true;
                isDying = false;
            }
        }
    }
}
