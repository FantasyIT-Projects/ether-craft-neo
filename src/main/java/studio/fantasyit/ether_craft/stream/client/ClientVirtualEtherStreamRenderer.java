package studio.fantasyit.ether_craft.stream.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.attachment.ChainedEmitterEntityHitCache;

public class ClientVirtualEtherStreamRenderer {

    private static final Identifier TEXTURE = EtherCraft.id("textures/particle/ether_stream.png");
    private static final RenderType RENDER_TYPE = RenderType.create(
            "ether_stream_tail_custom",
            RenderSetup.builder(EtherStreamRenderPipeline.ETHER_STREAM_ENTITY_PIPELINE)
                    .withTexture("Sampler0", TEXTURE)
                    .sortOnUpload()
                    .createRenderSetup()
    );

    private static final float LABEL_SCALE = 0.010416667F;

    public static void onRender(Minecraft mc, PoseStack poseStack, SubmitNodeCollector collector,CameraRenderState camera) {
        ClientVESHData data = ClientVESHData.get();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        for (var posEntry : data.getEntries().entrySet()) {
            ChainedEmitterEntityHitCache.PosDir posDir = posEntry.getKey();
            ClientVESHData.ClientVESHEntry veshEntry = posEntry.getValue();

            for (var streamEntry : veshEntry.streams.entrySet()) {
                int streamId = streamEntry.getKey();
                ClientStreamEntry stream = streamEntry.getValue();

                if (stream.isRemoved()) continue;

                long elapsed = mc.level.getGameTime() - stream.receivedAtTick;
                Vec3 currentPos = stream.startPos.add(
                        stream.motion.scale(stream.startTickCount + elapsed + partialTick)
                );

                // Render tail: 6 billboard quads backward from currentPos
                double speed = stream.motion.length();
                if (speed > 0.0001) {
                    Vec3 stepBack = stream.motion.reverse();
                    for (int i = 0; i < 6; i++) {
                        Vec3 tailPos = currentPos.add(stepBack.scale(i));
                        poseStack.pushPose();
                        float dx = (float) (tailPos.x - camera.pos.x);
                        float dy = (float) (tailPos.y - camera.pos.y);
                        float dz = (float) (tailPos.z - camera.pos.z);
                        poseStack.translate(dx, dy, dz);
                        poseStack.mulPose(camera.orientation);

                        float alpha = 1f - (float) i / 6.1f;
                        float size = 1.0f / (float) Math.pow(1.5, i);
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
                }

                // Render label at currentPos
                renderLabel(stream, currentPos, camera, poseStack, collector);
            }
        }
    }

    private static void renderLabel(ClientStreamEntry stream, Vec3 currentPos,
                                    CameraRenderState camera, PoseStack poseStack, SubmitNodeCollector collector) {
        if (stream.label == null) return;
        Vec3 motion = stream.motion;
        if (motion.lengthSqr() < 0.0001) return;

        Font font = Minecraft.getInstance().font;
        String fullText = stream.label.getString();
        int fullTextWidth = font.width(fullText);
        if (fullTextWidth == 0) return;

        // Simplified: render full text without left/right clipping
        String visibleText = fullText;
        int visibleTextWidth = fullTextWidth;

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
        for (Vec3 faceNormal : new Vec3[]{normal, normal.scale(-1)}) {
            poseStack.pushPose();

            poseStack.translate(
                    currentPos.x - camera.pos.x,
                    currentPos.y - camera.pos.y,
                    currentPos.z - camera.pos.z
            );

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
                    Font.DisplayMode.NORMAL, 0xF000F0, stream.labelColor, 0, 0);

            poseStack.popPose();
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, int a, float u, float v, int light) {
        buffer.addVertex(pose, x, y, 0f)
                .setColor(255, 255, 255, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
    }
}
