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
import studio.fantasyit.ether_craft.EtherCraft;

@OnlyIn(Dist.CLIENT)
@net.neoforged.fml.common.EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class EtherStreamSyncDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final int CREATE_COLOR = ARGB.colorFromFloat(1.0F, 0.0F, 1.0F, 0.0F);
    private static final int QUICK_CREATE_COLOR = ARGB.colorFromFloat(1.0F, 0.0F, 1.0F, 1.0F);
    private static final int UPDATE_COLOR = ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 0.0F);
    private static final int DELETE_COLOR = ARGB.colorFromFloat(1.0F, 1.0F, 0.0F, 0.0F);

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        if (!EtherStreamSyncMarker.isEnabled()) return;
        long now = net.minecraft.client.Minecraft.getInstance().level.getGameTime();
        for (EtherStreamSyncMarker.Marker marker : EtherStreamSyncMarker.getMarkers()) {
            int age = (int) (now - marker.gameTime());
            float progress = 1.0F - Math.max(0, age) / (float) EtherStreamSyncMarker.LIFETIME_TICKS;
            if (progress <= 0.0F) continue;
            int color = switch (marker.type()) {
                case CREATE -> CREATE_COLOR;
                case QUICK_CREATE -> QUICK_CREATE_COLOR;
                case UPDATE -> UPDATE_COLOR;
                case DELETE -> DELETE_COLOR;
            };
            float alpha = progress;
            float halfSize = 0.12F * progress;
            Vec3 pos = marker.pos();
            AABB box = new AABB(pos.x - halfSize, pos.y - halfSize, pos.z - halfSize,
                    pos.x + halfSize, pos.y + halfSize, pos.z + halfSize);
            Gizmos.cuboid(box, GizmoStyle.stroke(ARGB.multiplyAlpha(color, alpha)));
        }
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onRegisterDebugRenderers(net.neoforged.neoforge.client.event.RegisterDebugRenderersEvent event) {
        event.register(client -> new EtherStreamSyncDebugRenderer());
    }
}
