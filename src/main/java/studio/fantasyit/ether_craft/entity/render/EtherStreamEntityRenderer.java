package studio.fantasyit.ether_craft.entity.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.entity.EtherStreamEntity;
import org.joml.Vector3fc;

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
            Vector3fc sp = entity.getEntityData().get(EtherStreamEntity.LABEL_START_POS);
            state.startPos = new Vec3(sp.x(), sp.y(), sp.z());
        }
        state.labelColor = entity.getEntityData().get(EtherStreamEntity.LABEL_COLOR);
        state.motion = entity.getDeltaMovement();
        // --- End label extraction ---
    }

    @Override
    public void submit(EtherStreamEntityRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
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
        renderLabel(state, poseStack, camera);
        super.submit(state, poseStack, collector, camera);
    }

    private void renderLabel(EtherStreamEntityRenderState state, PoseStack poseStack, CameraRenderState camera) {
        if (state.label == null || state.startPos == null) return;
        Vec3 motion = state.motion;
        if (motion.lengthSqr() < 0.0001) return;

        Font font = Minecraft.getInstance().font;
        String fullText = state.label.getString();
        int fullTextWidth = font.width(fullText);
        if (fullTextWidth == 0) return;

        // Compute visible portion based on distance from start position
        double worldDist = state.startPos.distanceTo(new Vec3(state.x, state.y, state.z));
        float fontUnitsAvail = (float) (worldDist / LABEL_SCALE);
        int visibleWidth = Math.round(fontUnitsAvail);
        if (visibleWidth <= 0) return;

        // Get the rightmost portion of text that fits within the available pixel width
        String visibleText;
        boolean needsClipping;
        if (visibleWidth >= fullTextWidth) {
            visibleText = fullText;
            needsClipping = false;
        } else {
            visibleText = font.plainSubstrByWidth(fullText, visibleWidth, true);
            needsClipping = true;
        }
        if (visibleText.isEmpty()) return;

        int visibleTextWidth = font.width(visibleText);

        // Set up PoseStack: orient to orthogonal-of-motion plane
        poseStack.pushPose();

        Vec3 dir = motion.normalize();
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 normal;
        if (Math.abs(dir.dot(up)) > 0.999) {
            normal = dir.cross(new Vec3(1.0, 0.0, 0.0)).normalize();
        } else {
            normal = dir.cross(up).normalize();
        }
        Quaternionf rotation = new Quaternionf().rotateTo(
                new org.joml.Vector3f(0, 0, 1),
                new org.joml.Vector3f((float) normal.x, (float) normal.y, (float) normal.z));
        poseStack.mulPose(rotation);
        poseStack.scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);

        // Right edge of text aligns with entity origin, text extends leftward
        float textX = needsClipping ? -visibleTextWidth : -fullTextWidth;

        Matrix4f poseMatrix = poseStack.last().pose();
        try (ByteBufferBuilder builder = new ByteBufferBuilder(256)) {
            MultiBufferSource.BufferSource buf = MultiBufferSource.immediate(builder);
            font.drawInBatch(visibleText, textX, 0, state.labelColor, false, poseMatrix,
                    buf, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
            buf.endBatch();
        }

        poseStack.popPose();
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, int a, float u, float v, int light) {
        buffer.addVertex(pose, x, y, 0f).setColor(255, 255, 255, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f);
    }
}
