package studio.fantasyit.ether_craft.stream.client.extra;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.stream.client.data.ClientStreamEntry;

public interface IEtherStreamExtraClientLogic {
    boolean shouldDelayDeath(ClientStreamEntry entry);

    void onTick(ClientStreamEntry entry);

    void onRender(ClientStreamEntry stream, Vec3 currentPos, CameraRenderState camera, PoseStack poseStack, SubmitNodeCollector collector);
}
