package studio.fantasyit.ether_craft.plating.event;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.CamouflageState;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class WorldRenderEvent {

    @SubscribeEvent
    public static void onRenderCamouflage(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        for (Player player : mc.level.players()) {
            CamouflageState state = player.getExistingData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get()).orElse(null);
            if (state == null || !state.isActive()) continue;

            ChestModel cachedChestModel = new ChestModel(mc.getEntityModels().bakeLayer(
                    ChestRenderer.LAYERS.select(ChestType.SINGLE)));
            SpriteId cachedChestSprite = Sheets.chooseSprite(ChestRenderState.ChestMaterialType.REGULAR, ChestType.SINGLE);
            PoseStack poseStack = event.getPoseStack();

            SubmitNodeCollector collector = event.getSubmitNodeCollector();
            CameraRenderState camera = event.getLevelRenderState().cameraRenderState;

            BlockPos pos = state.camouflagePos();
            Direction dir = Direction.fromYRot(state.camouflageYaw());

            poseStack.pushPose();
            poseStack.translate(
                    pos.getX() - camera.pos.x,
                    pos.getY() - camera.pos.y,
                    pos.getZ() - camera.pos.z + 1
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
