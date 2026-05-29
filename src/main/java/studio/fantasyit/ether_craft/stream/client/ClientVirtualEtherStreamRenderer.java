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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import studio.fantasyit.ether_craft.EtherCraft;

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

    public static void onRender(Minecraft mc, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        ClientVESHData data = ClientVESHDataGetter.get();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Quaternionf invOrientation = camera.orientation.conjugate(new Quaternionf());
        Vector3f offsetVec = new Vector3f();

        poseStack.pushPose();
        poseStack.mulPose(camera.orientation);

        collector.order(1).submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            Vector3f normal = pose.transformNormal(0, 1f, 0, new Vector3f());
            for (var posEntry : data.getEntries().entrySet()) {
                ClientVESHData.ClientVESHEntry veshEntry = posEntry.getValue();
                for (var streamEntry : veshEntry.streams.entrySet()) {
                    ClientStreamEntry stream = streamEntry.getValue();
                    if (stream.isRemoved()) continue;

                    long elapsed = mc.level.getGameTime() - stream.receivedAtTick;
                    Vec3 currentPos = stream.startPos.add(
                            stream.motion.scale(stream.startTickCount + elapsed));//这里不加pt；

                    double speed = stream.motion.length();
                    if (speed <= 0.0001) continue;

                    Vec3 baseStep = stream.motion.reverse();
                    Vec3 tailEnd = currentPos.add(baseStep.scale(5));
                    if (!camera.cullFrustum.isVisible(new AABB(currentPos, tailEnd).inflate(0.5))) continue;

                    for (int i = 0; i < 6; i++) {
                        Vec3 tailPos = currentPos.add(baseStep.scale(i));
                        offsetVec.set(
                                (float) (tailPos.x - camera.pos.x),
                                (float) (tailPos.y - camera.pos.y),
                                (float) (tailPos.z - camera.pos.z));
                        invOrientation.transform(offsetVec);

                        float alpha = 1f - (float) i / 6.1f;
                        float szFact = (float) (0.03f * Math.log10(stream.ether));

                        float halfWidth = szFact * 0.5f / (float) Math.pow(1.5, i);
                        int a = (int) (alpha * 255);
                        int light = 0xF000F0;

                        vertex(buffer, pose, -halfWidth + offsetVec.x, -halfWidth + offsetVec.y, offsetVec.z, a, 1, 1, light, normal);
                        vertex(buffer, pose, halfWidth + offsetVec.x, -halfWidth + offsetVec.y, offsetVec.z, a, 0, 1, light, normal);
                        vertex(buffer, pose, halfWidth + offsetVec.x, halfWidth + offsetVec.y, offsetVec.z, a, 0, 0, light, normal);
                        vertex(buffer, pose, -halfWidth + offsetVec.x, halfWidth + offsetVec.y, offsetVec.z, a, 1, 0, light, normal);
                    }
                }
            }
        });

        poseStack.popPose();

        for (var posEntry : data.getEntries().entrySet()) {
            ClientVESHData.ClientVESHEntry veshEntry = posEntry.getValue();
            for (var streamEntry : veshEntry.streams.entrySet()) {
                ClientStreamEntry stream = streamEntry.getValue();
                if (stream.isRemoved()) continue;

                long elapsed = mc.level.getGameTime() - stream.receivedAtTick;
                Vec3 currentPos = stream.startPos.add(
                        stream.motion.scale(stream.startTickCount + elapsed + partialTick));

                double speed = stream.motion.length();
                if (speed > 0.0001) {
                    Vec3 tailEnd = currentPos.add(stream.motion.reverse().scale(5));
                    if (!camera.cullFrustum.isVisible(new AABB(currentPos, tailEnd).inflate(0.5))) continue;
                } else {
                    if (!camera.cullFrustum.isVisible(new AABB(currentPos, currentPos).inflate(0.5))) continue;
                }

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

        String visibleText = fullText;
        int visibleTextWidth = fullTextWidth;

        if (stream.isDying) {
            // Right-clip: deathTick counts down 60->0, consume text from right
            float progress = (60f - stream.deathTick) / 60f;
            int clipPixels = Math.min((int) (progress * fullTextWidth), fullTextWidth);
            visibleText = font.plainSubstrByWidth(fullText, Math.max(0, fullTextWidth - clipPixels));
            visibleTextWidth = font.width(visibleText);
        }

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

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, int a, float u, float v, int light, Vector3f norm) {
        buffer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(norm.x, norm.y, norm.z);
    }
}
