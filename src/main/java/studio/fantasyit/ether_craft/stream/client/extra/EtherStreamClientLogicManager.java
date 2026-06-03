package studio.fantasyit.ether_craft.stream.client.extra;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.stream.client.data.ClientStreamEntry;

import java.util.ArrayList;
import java.util.List;

public class EtherStreamClientLogicManager {
    public static List<IEtherStreamExtraClientLogic> extraLogic = new ArrayList<>();

    public static void collect() {
        extraLogic.add(new EtherStreamLabelLogic());
        extraLogic.add(new EtherStreamCarriedEntityLogic());
    }

    public static boolean shouldDelayDeath(ClientStreamEntry entry) {
        for (IEtherStreamExtraClientLogic logic : extraLogic) {
            if (logic.shouldDelayDeath(entry)) {
                return true;
            }
        }
        return false;
    }

    public static void onTick(ClientStreamEntry entry) {
        for (IEtherStreamExtraClientLogic logic : extraLogic) {
            logic.onTick(entry);
        }
    }

    public static void renderExtra(ClientStreamEntry stream, Vec3 currentPos, CameraRenderState camera, PoseStack poseStack, SubmitNodeCollector collector) {
        for (IEtherStreamExtraClientLogic logic : extraLogic) {
            logic.onRender(stream, currentPos, camera, poseStack, collector);
        }
    }

    public static boolean shouldRender(ClientStreamEntry entry) {
        for (IEtherStreamExtraClientLogic logic : extraLogic) {
            if (!logic.shouldRender(entry)) {
                return false;
            }
        }
        return true;
    }

    public static void onDestroy(ClientStreamEntry entry) {
        for (IEtherStreamExtraClientLogic logic : extraLogic) {
            logic.onDestroy(entry);
        }
    }
}
