package studio.fantasyit.ether_craft.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.base.IWorldRenderBE;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class WorldRenderEvent {
    @SubscribeEvent
    public static void onRenderNameTags(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.hitResult == null)
            return;
        if (!(mc.hitResult instanceof BlockHitResult blockHit))
            return;

        BlockPos pos = blockHit.getBlockPos();
        BlockEntity be = mc.level.getBlockEntity(pos);
        if (!(be instanceof IWorldRenderBE renderBe))
            return;

        Component name = renderBe.getRenderName();
        if (name == null)
            return;

        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;

        float dx = (float) (pos.getX() + 0.5 - camera.pos.x);
        float dy = (float) (pos.getY() + 0.5 - camera.pos.y);
        float dz = (float) (pos.getZ() + 0.5 - camera.pos.z);

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);

        int light = LevelRenderer.getLightCoords(mc.level, pos.above());
        double distSq = camera.pos.distanceToSqr(pos.getCenter());
        Vec3 nameTagAttachment = new Vec3(0, 0.6, 0);

        collector.order(1).submitNameTag(
                poseStack, nameTagAttachment, 0, name, false, light, distSq, camera
        );

        poseStack.popPose();
    }
}
