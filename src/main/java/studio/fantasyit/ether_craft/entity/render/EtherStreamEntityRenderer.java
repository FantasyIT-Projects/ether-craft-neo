package studio.fantasyit.ether_craft.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.entity.EtherStreamEntity;

public class EtherStreamEntityRenderer extends EntityRenderer<EtherStreamEntity, EtherStreamEntityRenderState> {
    private static final Identifier TEXTURE = EtherCraft.id("textures/particle/ether_stream.png");
    private static final RenderType RENDER_TYPE = RenderType.create(
            "ether_stream_tail",
            RenderSetup.builder(EtherStreamRenderPipeline.ETHER_STREAM_ENTITY_PIPELINE)
                    .withTexture("Sampler0", TEXTURE)
                    .sortOnUpload()
                    .createRenderSetup()
    );

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
        super.submit(state, poseStack, collector, camera);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, int a, float u, float v, int light) {
        buffer.addVertex(pose, x, y, 0f).setColor(255, 255, 255, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f);
    }
}
