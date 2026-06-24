package studio.fantasyit.ether_craft.stream.client.extra;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.stream.client.data.ClientStreamEntry;

public interface IEtherStreamExtraClientLogic {
    boolean shouldAttach(ClientStreamEntry entry);

    default boolean shouldDelayDeath(ClientStreamEntry entry) {
        return false;
    }

    default boolean shouldAlwaysRender(ClientStreamEntry entry, Vec3 currentPos, CameraRenderState camera) {
        return false;
    }

    default void onTick(ClientStreamEntry entry) {
    }

    default void onRender(ClientStreamEntry stream, Vec3 currentPos, CameraRenderState camera, PoseStack poseStack, SubmitNodeCollector collector) {
    }

    default boolean shouldRender(ClientStreamEntry entry) {
        return true;
    }

    default void onDestroy(ClientStreamEntry entry) {
    }

    default void onDetach(ClientStreamEntry entry) {
    }

    default void onAttach(ClientStreamEntry entry) {
    }
}
