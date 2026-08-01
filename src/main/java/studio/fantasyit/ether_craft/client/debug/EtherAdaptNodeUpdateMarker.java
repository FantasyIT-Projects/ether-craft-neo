package studio.fantasyit.ether_craft.client.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class EtherAdaptNodeUpdateMarker {
    public enum Type { EAN_EXTRA, PLUGIN_DATA, ETHER_VALUE }

    public record Marker(Type type, Vec3 pos, long gameTime) {}

    public static final int LIFETIME_TICKS = 20;

    private static boolean ENABLED = false;
    private static final List<Marker> markers = new ArrayList<>();

    private EtherAdaptNodeUpdateMarker() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static void setEnabled(boolean enabled) {
        ENABLED = enabled;
    }

    public static void record(Type type, Vec3 pos) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        markers.add(new Marker(type, pos, mc.level.getGameTime()));
    }

    public static void tick() {
        if (markers.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        markers.removeIf(m -> now - m.gameTime() > LIFETIME_TICKS);
    }

    public static void clear() {
        markers.clear();
    }

    public static List<Marker> getMarkers() {
        return markers;
    }
}
