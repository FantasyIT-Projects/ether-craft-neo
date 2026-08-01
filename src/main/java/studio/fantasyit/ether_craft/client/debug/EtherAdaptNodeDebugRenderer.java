package studio.fantasyit.ether_craft.client.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import studio.fantasyit.ether_craft.EtherCraft;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class EtherAdaptNodeDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final int EAN_EXTRA_COLOR = ARGB.colorFromFloat(1.0F, 1.0F, 0.0F, 0.0F);
    private static final int PLUGIN_DATA_COLOR = ARGB.colorFromFloat(1.0F, 0.0F, 0.0F, 1.0F);
    private static final int ETHER_VALUE_COLOR = ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 0.0F);
    private static final float[] BASE_HALF_SIZE = new float[]{
            0.15F, 0.30F, 0.45F
    };

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        if (!EtherAdaptNodeUpdateMarker.isEnabled()) return;
        long now = net.minecraft.client.Minecraft.getInstance().level.getGameTime();
        for (EtherAdaptNodeUpdateMarker.Marker marker : EtherAdaptNodeUpdateMarker.getMarkers()) {
            int age = (int) (now - marker.gameTime());
            float progress = 1.0F - Math.max(0, age) / (float) EtherAdaptNodeUpdateMarker.LIFETIME_TICKS;
            if (progress <= 0.0F) continue;
            int color = switch (marker.type()) {
                case EAN_EXTRA -> EAN_EXTRA_COLOR;
                case PLUGIN_DATA -> PLUGIN_DATA_COLOR;
                case ETHER_VALUE -> ETHER_VALUE_COLOR;
            };
            float alpha = progress;
            float halfSize = BASE_HALF_SIZE[marker.type().ordinal()] * progress;
            Vec3 pos = marker.pos();
            AABB box = new AABB(pos.x - halfSize, pos.y - halfSize, pos.z - halfSize,
                    pos.x + halfSize, pos.y + halfSize, pos.z + halfSize);
            Gizmos.cuboid(box, GizmoStyle.stroke(ARGB.multiplyAlpha(color, alpha))).setAlwaysOnTop();
        }
    }

    @SubscribeEvent
    public static void onRegisterDebugRenderers(net.neoforged.neoforge.client.event.RegisterDebugRenderersEvent event) {
        event.register(client -> new EtherAdaptNodeDebugRenderer());
    }
}
