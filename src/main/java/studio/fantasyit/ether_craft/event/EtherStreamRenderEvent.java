package studio.fantasyit.ether_craft.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.client.render.ClientVirtualEtherStreamRenderer;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class EtherStreamRenderEvent {
    @SubscribeEvent
    public static void onRender(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
        ClientVirtualEtherStreamRenderer.onRender(mc, poseStack, collector, camera);
    }
}
