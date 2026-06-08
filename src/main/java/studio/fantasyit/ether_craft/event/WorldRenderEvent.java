package studio.fantasyit.ether_craft.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.base.IWorldRenderBE;
import studio.fantasyit.ether_craft.plating.CamouflageState;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

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

    private static ChestModel cachedChestModel;
    private static SpriteId cachedChestSprite;

    @SubscribeEvent
    public static void onRenderCamouflage(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        for (Player player : mc.level.players()) {
            CamouflageState state = player.getExistingData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get()).orElse(null);
            if (state == null || !state.isActive()) continue;

            if (cachedChestModel == null) {
                cachedChestModel = new ChestModel(mc.getEntityModels().bakeLayer(
                        ChestRenderer.LAYERS.select(ChestType.SINGLE)));
                cachedChestSprite = Sheets.chooseSprite(ChestRenderState.ChestMaterialType.REGULAR, ChestType.SINGLE);
            }

            PoseStack poseStack = event.getPoseStack();
            SubmitNodeCollector collector = event.getSubmitNodeCollector();
            CameraRenderState camera = event.getLevelRenderState().cameraRenderState;

            BlockPos pos = state.camouflagePos();
            Direction dir = Direction.fromYRot(state.camouflageYaw());

            poseStack.pushPose();
            poseStack.translate(
                    pos.getX() + 0.5 - camera.pos.x,
                    pos.getY() - camera.pos.y,
                    pos.getZ() + 0.5 - camera.pos.z
            );
            poseStack.mulPose(Axis.YP.rotationDegrees(-dir.toYRot()));

            int light = LevelRenderer.getLightCoords(mc.level, pos);

            collector.submitModel(cachedChestModel, 0.0f, poseStack,
                    light, OverlayTexture.NO_OVERLAY, -1,
                    cachedChestSprite, mc.getAtlasManager(), 0, null);

            poseStack.popPose();
        }
    }
}
