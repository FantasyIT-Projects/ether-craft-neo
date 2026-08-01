package studio.fantasyit.ether_craft.client.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class EtherStreamSyncMarker {
    public enum Type { CREATE, QUICK_CREATE, UPDATE, DELETE }

    public record Marker(Type type, Vec3 pos, int streamId, long gameTime) {}

    public static final int LIFETIME_TICKS = 20;

    private static boolean ENABLED = false;
    private static final List<Marker> markers = new ArrayList<>();

    private EtherStreamSyncMarker() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static void setEnabled(boolean enabled) {
        ENABLED = enabled;
    }

    public static void record(Type type, Vec3 pos, int streamId) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        markers.add(new Marker(type, pos, streamId, mc.level.getGameTime()));
    }

    public static void tick() {
        if (markers.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        markers.removeIf(m -> now - m.gameTime() > LIFETIME_TICKS);
    }

    public static List<Marker> getMarkers() {
        return markers;
    }
}
