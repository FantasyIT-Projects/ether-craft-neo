package studio.fantasyit.ether_craft.event;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache.PosDir;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamCreateS2C;
import studio.fantasyit.ether_craft.network.s2c.EtherStreamUpdateS2C;

import java.util.*;

public class ClientVESHData {
    public static class ClientStreamEntry {
        Vec3 startPos = Vec3.ZERO;
        Vec3 motion = Vec3.ZERO;
        int startTickCount;
        int ether;
        long receivedAtTick;
        byte flags;
        int deathTick;
        @Nullable
        Component label;
        int labelColor;
        boolean removed;

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
    }

    public static class ClientVESHEntry {
        final Map<Integer, ClientStreamEntry> streams = new HashMap<>();
    }

    private final Map<PosDir, ClientVESHEntry> entries = new HashMap<>();

    public void handleCreate(EtherStreamCreateS2C msg) {
        ClientVESHEntry entry = entries.computeIfAbsent(msg.posDir(), k -> new ClientVESHEntry());
        if (!entry.streams.containsKey(msg.streamId())) {
            entry.streams.put(msg.streamId(), new ClientStreamEntry(msg));
        }
    }

    public void handleUpdate(EtherStreamUpdateS2C msg) {
        ClientVESHEntry entry = entries.computeIfAbsent(msg.posDir(), k -> new ClientVESHEntry());
        Map<Integer, ClientStreamEntry> streams = entry.streams;
        Set<Integer> seen = new HashSet<>();

        for (EtherStreamUpdateS2C.StreamEntry update : msg.entries()) {
            seen.add(update.streamId());
            ClientStreamEntry current = streams.get(update.streamId());

            if (current == null) {
                current = new ClientStreamEntry(null);
                streams.put(update.streamId(), current);
            }

            if (update.isDead() && !update.isDying()) {
                current.removed = true;
                continue;
            }
            current.startTickCount = update.tickCount();
            current.ether = update.ether();
            current.label = update.label();
            current.labelColor = update.labelColor();
            current.receivedAtTick = Minecraft.getInstance().level != null
                    ? Minecraft.getInstance().level.getGameTime() : 0;
            if (update.isDying()) {
                current.flags = update.flags();
                current.deathTick = update.deathTick();
            }
        }

        // Remove streams not in this update batch
        streams.entrySet().removeIf(e -> !seen.contains(e.getKey()));

        // Remove empty entries
        if (entry.streams.isEmpty()) {
            entries.remove(msg.posDir());
        }
    }

    public Map<PosDir, ClientVESHEntry> getEntries() {
        return entries;
    }

    public static ClientVESHData get() {
        Level level = Minecraft.getInstance().level;
        if (level == null) return new ClientVESHData();
        return CACHE.computeIfAbsent(level, k -> new ClientVESHData());
    }

    private static final Map<Level, ClientVESHData> CACHE = new WeakHashMap<>();
}
