package studio.fantasyit.ether_craft.plating.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.CamouflageState;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class PlayerRendererEvent {
    private static final ContextKey<Boolean> SHOULD_HIDE = new ContextKey<>(EtherCraft.id("camouflage_hide"));
    private static final ContextKey<BlockPos> CAMOUFLAGE_POS = new ContextKey<>(EtherCraft.id("camouflage_pos"));
    private static final ContextKey<Double> CAMOUFLAGE_OFF_X = new ContextKey<>(EtherCraft.id("camouflage_off_x"));
    private static final ContextKey<Double> CAMOUFLAGE_OFF_Y = new ContextKey<>(EtherCraft.id("camouflage_off_y"));
    private static final ContextKey<Double> CAMOUFLAGE_OFF_Z = new ContextKey<>(EtherCraft.id("camouflage_off_z"));
    private static final ContextKey<Float> CAMOUFLAGE_YAW_KEY = new ContextKey<>(EtherCraft.id("camouflage_yaw"));

    @SubscribeEvent
    public static void playerExtractRenderStateModifier(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState renderState) {
                CamouflageState data = avatar.getExistingData(AttachmentDataRegistry.CAMOUFLAGE_STATE).orElse(null);
                if (data != null && data.isActive()) {
                    renderState.setRenderData(SHOULD_HIDE, true);
                    renderState.setRenderData(CAMOUFLAGE_POS, data.camouflagePos());
                    renderState.setRenderData(CAMOUFLAGE_YAW_KEY, data.camouflageYaw());
                    Entity entity = (Entity) avatar;
                    renderState.setRenderData(CAMOUFLAGE_OFF_X, data.camouflagePos().getX() + 1 - entity.position().x);
                    renderState.setRenderData(CAMOUFLAGE_OFF_Y, data.camouflagePos().getY() - entity.position().y);
                    renderState.setRenderData(CAMOUFLAGE_OFF_Z, data.camouflagePos().getZ() + 1 - entity.position().z);
                } else {
                    renderState.setRenderData(SHOULD_HIDE, false);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre<?> event) {
        AvatarRenderState renderState = event.getRenderState();
        Boolean shouldHide = renderState.getRenderData(SHOULD_HIDE);
        if (shouldHide == null || !shouldHide) return;

        event.setCanceled(true);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        BlockPos pos = renderState.getRenderData(CAMOUFLAGE_POS);
        Float yaw = renderState.getRenderData(CAMOUFLAGE_YAW_KEY);
        Double offX = renderState.getRenderData(CAMOUFLAGE_OFF_X);
        Double offY = renderState.getRenderData(CAMOUFLAGE_OFF_Y);
        Double offZ = renderState.getRenderData(CAMOUFLAGE_OFF_Z);
        if (pos == null || yaw == null || offX == null || offY == null || offZ == null) return;

        ChestModel chestModel = new ChestModel(mc.getEntityModels().bakeLayer(
                ChestRenderer.LAYERS.select(ChestType.SINGLE)));
        SpriteId sprite = Sheets.chooseSprite(ChestRenderState.ChestMaterialType.REGULAR, ChestType.SINGLE);

        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        Direction dir = Direction.fromYRot(yaw);

        poseStack.pushPose();
        poseStack.translate(offX - 0.5, offY, offZ - 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-dir.toYRot()));
        poseStack.translate(-0.5, 0, -0.5);

        int light = LevelRenderer.getLightCoords(mc.level, pos);

        collector.submitModel(chestModel, 0.0f, poseStack,
                light, OverlayTexture.NO_OVERLAY, -1,
                sprite, mc.getAtlasManager(), 0, null);

        poseStack.popPose();
    }
}
