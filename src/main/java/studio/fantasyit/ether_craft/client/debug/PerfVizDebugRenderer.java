package studio.fantasyit.ether_craft.client.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHData;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHEntry;
import studio.fantasyit.ether_craft.stream.client.data.ClientStreamEntry;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class PerfVizDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final int LINE_COLOR = ARGB.colorFromFloat(1.0F, 0.0F, 0.9F, 1.0F);
    private static final int GOOD_COLOR = ARGB.colorFromFloat(1.0F, 0.0F, 1.0F, 0.0F);
    private static final int YELLOW_COLOR = ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 0.0F);
    private static final int ORANGE_COLOR = ARGB.colorFromFloat(1.0F, 1.0F, 0.5F, 0.0F);
    private static final int BAD_COLOR = ARGB.colorFromFloat(1.0F, 1.0F, 0.0F, 0.0F);
    private static final long YELLOW_THRESHOLD_NANOS = 10_000L;
    private static final long ORANGE_THRESHOLD_NANOS = 20_000L;
    private static final long RED_THRESHOLD_NANOS = 50_000L;

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        if (!PerfVizData.isEnabled()) return;
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        for (ClientVESHEntry vesh : ClientVESHData.getWithCurrentLevel(level).getEntriesIterable()) {
            ClientStreamEntry far = null;
            double farDist = -1;
            for (ClientStreamEntry stream : vesh.steamsIterable) {
                if (stream.isDying || stream.removed) continue;
                double d = stream.startPos.distanceToSqr(stream.currentPos);
                if (d > farDist) {
                    farDist = d;
                    far = stream;
                }
            }
            if (far == null) continue;
            Gizmos.line(far.startPos, far.currentPos, LINE_COLOR, 2.5F);
            PerfVizData.Stat veshStat = PerfVizData.getVeshStats().get(vesh.posDir);
            if (veshStat != null) {
                Vec3 mid = far.startPos.add(far.currentPos).scale(0.5);
                Gizmos.billboardText(format(veshStat), mid,
                        TextGizmo.Style.forColorAndCentered(ARGB.multiplyAlpha(LINE_COLOR, 0.9F)).withScale(0.5F));
            }
        }

        for (var e : PerfVizData.getBlockStats().object2ObjectEntrySet()) {
            BlockPos pos = e.getKey();
            PerfVizData.Stat stat = e.getValue();
            Gizmos.billboardText(format(stat), Vec3.atLowerCornerWithOffset(pos, 0.5, 1.3, 0.5),
                    TextGizmo.Style.forColorAndCentered(colorFor(stat)).withScale(0.2F));
        }
    }

    private static int colorFor(PerfVizData.Stat stat) {
        long avg = stat.avgNanos();
        if (avg < YELLOW_THRESHOLD_NANOS) return GOOD_COLOR;
        if (avg < ORANGE_THRESHOLD_NANOS) return YELLOW_COLOR;
        if (avg < RED_THRESHOLD_NANOS) return ORANGE_COLOR;
        return BAD_COLOR;
    }

    private static String format(PerfVizData.Stat stat) {
        return micros(stat.avgNanos()) + "us(" + micros(stat.maxNanos()) + "us)";
    }

    private static long micros(long nanos) {
        return nanos / 1000L;
    }

    @SubscribeEvent
    public static void onRegisterDebugRenderers(net.neoforged.neoforge.client.event.RegisterDebugRenderersEvent event) {
        event.register(client -> new PerfVizDebugRenderer());
    }
}
