package studio.fantasyit.ether_craft.entity.stream.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3fc;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.entity.stream.EtherStreamEntity;

public class EtherStreamEntityRenderer extends EntityRenderer<EtherStreamEntity, EtherStreamEntityRenderState> {
    private static final Identifier TEXTURE = EtherCraft.id("textures/particle/ether_stream.png");
    private static final RenderType RENDER_TYPE = RenderType.create(
            "ether_stream_tail",
            RenderSetup.builder(EtherStreamRenderPipeline.ETHER_STREAM_ENTITY_PIPELINE)
                    .withTexture("Sampler0", TEXTURE)
                    .sortOnUpload()
                    .createRenderSetup()
    );

    private static final float LABEL_SCALE = 0.010416667F;

    public EtherStreamEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EtherStreamEntityRenderState createRenderState() {
        return new EtherStreamEntityRenderState();
    }

    @Override
    public void extractRenderState(EtherStreamEntity entity, EtherStreamEntityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tailCount = entity.tailCount;
        for (int i = 0; i < entity.tailCount; i++) {
            int idx = (entity.tailHead - i + EtherStreamEntity.MAX_TAIL) % EtherStreamEntity.MAX_TAIL;
            state.tailX[i] = entity.tailX[idx];
            state.tailY[i] = entity.tailY[idx];
            state.tailZ[i] = entity.tailZ[idx];
            state.tailSize[i] = entity.tailSize[idx];
        }
        // --- Label extraction ---
        java.util.Optional<net.minecraft.network.chat.Component> labelData = entity.getEntityData().get(EtherStreamEntity.LABEL_DATA);
        labelData.ifPresent(label -> state.label = label);
        if (labelData.isPresent()) {
            Vector3fc sp = entity.getEntityData().get(EtherStreamEntity.START_POS);
            state.startPos = new Vec3(sp.x(), sp.y(), sp.z());
        }
        state.labelColor = entity.getEntityData().get(EtherStreamEntity.LABEL_COLOR);
        state.motion = entity.getDeltaMovement();
        state.dying = entity.getEntityData().get(EtherStreamEntity.DYING);
        if (state.dying) {
            state.deathTick = entity.clientDeathTick;
            Vector3fc dp = entity.getEntityData().get(EtherStreamEntity.DEATH_POS);
            state.deathPos = new Vec3(dp.x(), dp.y(), dp.z());
        }
        state.speed = (float) state.motion.length();
        // --- End label extraction ---
    }

    @Override
    public void submit(EtherStreamEntityRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.dying)
            for (int i = 0; i < state.tailCount; i++) {
                poseStack.pushPose();
                float dx = (float) (state.tailX[i] - state.x);
                float dy = (float) (state.tailY[i] - state.y);
                float dz = (float) (state.tailZ[i] - state.z);
                poseStack.translate(dx, dy, dz);
                poseStack.mulPose(camera.orientation);

                int age = i;
                float alpha = 1f - (float) age / 6.1f;
                float size = state.tailSize[i] / (float) Math.pow(1.5, age);
                poseStack.scale(size, size, 1f);

                int a = (int) (alpha * 255);
                int light = 0xF000F0;

                collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
                    vertex(buffer, pose, -0.5f, -0.5f, a, 1, 1, light);
                    vertex(buffer, pose, 0.5f, -0.5f, a, 0, 1, light);
                    vertex(buffer, pose, 0.5f, 0.5f, a, 0, 0, light);
                    vertex(buffer, pose, -0.5f, 0.5f, a, 1, 0, light);
                });

                poseStack.popPose();
            }
        renderLabel(state, poseStack, collector, camera);
        super.submit(state, poseStack, collector, camera);
    }

    private void renderLabel(EtherStreamEntityRenderState state, PoseStack poseStack,
                             SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.label == null) return;
        Vec3 motion = state.motion;
        if (motion.lengthSqr() < 0.0001) return;

        Font font = Minecraft.getInstance().font;
        String fullText = state.label.getString();
        int fullTextWidth = font.width(fullText);
        if (fullTextWidth == 0) return;

        String visibleText;
        int visibleTextWidth;

        if (state.dying) {
            // Death: consume characters from the right as text continues moving
            int consumedRight = Math.round(state.deathTick * state.speed / LABEL_SCALE);
            int remainingWidth = fullTextWidth - consumedRight;
            if (remainingWidth <= 0) return;
            visibleText = font.plainSubstrByWidth(fullText, remainingWidth, false);
            if (visibleText.isEmpty()) return;
            visibleTextWidth = font.width(visibleText);
        } else {
            // Alive: clip from the left at start position
            if (state.startPos == null) return;
            double worldDist = state.startPos.distanceTo(new Vec3(state.x, state.y, state.z));
            float fontUnitsAvail = (float) (worldDist / LABEL_SCALE);
            int visibleWidth = Math.round(fontUnitsAvail);
            if (visibleWidth <= 0) return;
            if (visibleWidth >= fullTextWidth) {
                visibleText = fullText;
            } else {
                visibleText = font.plainSubstrByWidth(fullText, visibleWidth, true);
            }
            if (visibleText.isEmpty()) return;
            visibleTextWidth = font.width(visibleText);
        }

        // Compute normal once
        Vec3 dir = motion.normalize();
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        boolean vertical = Math.abs(dir.dot(up)) > 0.999;
        Vec3 normal;
        if (vertical) {
            normal = dir.cross(new Vec3(1.0, 0.0, 0.0)).normalize();
        } else {
            normal = dir.cross(up).normalize();
        }

        FormattedCharSequence text = FormattedCharSequence.forward(visibleText, net.minecraft.network.chat.Style.EMPTY);

        // Render on both faces so the label is visible from either side
        // For normal face: right-align (text extends left from entity), for -normal face: left-align
        for (Vec3 faceNormal : new Vec3[]{normal, normal.scale(-1)}) {
            poseStack.pushPose();

            if (state.dying && state.deathPos != null) {
                poseStack.translate(state.deathPos.x - state.x, state.deathPos.y - state.y, state.deathPos.z - state.z);
            }

            Quaternionf rotation = new Quaternionf().rotateTo(
                    new org.joml.Vector3f(0, 0, 1),
                    new org.joml.Vector3f((float) faceNormal.x, (float) faceNormal.y, (float) faceNormal.z));
            poseStack.mulPose(rotation);
            if (vertical) {
                poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(faceNormal == normal ? -90 : 90)));
            }
            poseStack.scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);

            float textX;
            if (vertical) {
                textX = faceNormal == normal ? 0 : -visibleTextWidth;
            } else {
                textX = faceNormal == normal ? -visibleTextWidth : 0;
            }
            collector.submitText(poseStack, textX, 0, text, false,
                    Font.DisplayMode.NORMAL, 0xF000F0, state.labelColor, 0, 0);

            poseStack.popPose();
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, int a, float u, float v, int light) {
        buffer.addVertex(pose, x, y, 0f).setColor(255, 255, 255, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f);
    }
}
