package studio.fantasyit.ether_craft.stream.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHData;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHDataGetter;

public class ClientVirtualEtherStreamRenderer {

    private static final Identifier TEXTURE = EtherCraft.id("textures/particle/ether_stream.png");
    private static final RenderType RENDER_TYPE = RenderType.create(
            "ether_stream_tail_custom",
            RenderSetup.builder(EtherStreamRenderPipeline.ETHER_STREAM_ENTITY_PIPELINE)
                    .withTexture("Sampler0", TEXTURE)
                    .sortOnUpload()
                    .createRenderSetup()
    );
    private static final int[] ALPHAS = new int[]{255, 213, 170, 128, 85, 42};

    public static void onRender(Minecraft mc, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        ClientVESHData data = ClientVESHDataGetter.get();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        Vector3f cameraForward = new Vector3f(0, 0, -1);
        camera.orientation.transform(cameraForward);
        float fx = cameraForward.x(), fy = cameraForward.y(), fz = cameraForward.z();

        Vector3f cameraRight = new Vector3f(1, 0, 0);
        camera.orientation.transform(cameraRight);
        Vector3f cameraUp = new Vector3f(0, 1, 0);
        camera.orientation.transform(cameraUp);
        collector.order(1).submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            data.startRenderStamp();
            int targetCount = 0;
            int particleCount = 0;
            float[] sizeFactor = new float[6];
            for (var i = 0; i < 6; i++) sizeFactor[i] = (float) (0.5f / Math.pow(1.5, i));

            double camX = camera.pos.x;
            double camY = camera.pos.y;
            double camZ = camera.pos.z;

            float camRightX = cameraRight.x;
            float camRightY = cameraRight.y;
            float camRightZ = cameraRight.z;

            float camUpX = cameraUp.x;
            float camUpY = cameraUp.y;
            float camUpZ = cameraUp.z;


            data.renderStamp(0);
            for (var veshEntry : data.getEntriesIterable()) {
                for (var stream : veshEntry.steamsIterable) {
                    if (stream.isDying() || !stream.shouldRender) {
                        continue;
                    }
                    data.renderStamp(1);

                    Vec3 currentPos = stream.currentPos;
                    double dx = currentPos.x - camX;
                    double dy = currentPos.y - camY;
                    double dz = currentPos.z - camZ;
                    double distance = dx * dx + dy * dy + dz * dz;
                    double dot = dx * fx + dy * fy + dz * fz;
                    data.renderStamp(2);
                    if (dot < -10.0) {
                        continue;
                    }

                    if (distance > 9000) {
                        if (stream.id % 4 != 0) continue;
                    } else if (distance > 1600) {
                        if (stream.id % 2 != 0) continue;
                    }
                    data.renderStamp(3);
                    Vec3 tailEnd = currentPos.add(stream.reverseStepMotions[5]);
                    if (!camera.cullFrustum.pointInFrustum(currentPos.x, currentPos.y, currentPos.z)
                            && !camera.cullFrustum.pointInFrustum(tailEnd.x, tailEnd.y, tailEnd.z)) {
                        data.renderStamp(4);
                        continue;
                    }
                    data.renderStamp(4);
                    targetCount++;
                    float szFact = (float) (0.03f * Math.log10(stream.ether));
                    for (int i = 0; i < 6; i++) {
                        Vec3 tailPos = currentPos.add(stream.reverseStepMotions[i]);
                        if (i != 0 && distance > 225 * (5 - i)) break;
                        data.renderStamp();
                        particleCount++;

                        float wx = (float) (tailPos.x - camX);
                        float wy = (float) (tailPos.y - camY);
                        float wz = (float) (tailPos.z - camZ);

                        data.renderStamp(5);
                        float halfWidth = szFact * sizeFactor[i];
                        int light = 0xF000F0;
                        int packedColor = ARGB.color(ALPHAS[i], 255, 255, 255);

                        float crx = camRightX, cry = camRightY, crz = camRightZ;
                        float cux = camUpX, cuy = camUpY, cuz = camUpZ;

                        data.renderStamp(6);
                        buffer.addVertex(
                                wx - halfWidth * crx - halfWidth * cux,
                                wy - halfWidth * cry - halfWidth * cuy,
                                wz - halfWidth * crz - halfWidth * cuz,
                                packedColor, 1, 1, OverlayTexture.NO_OVERLAY, light,
                                cux, cuy, cuz);
                        buffer.addVertex(
                                wx + halfWidth * crx - halfWidth * cux,
                                wy + halfWidth * cry - halfWidth * cuy,
                                wz + halfWidth * crz - halfWidth * cuz,
                                packedColor, 0, 1, OverlayTexture.NO_OVERLAY, light,
                                cux, cuy, cuz);
                        buffer.addVertex(
                                wx + halfWidth * crx + halfWidth * cux,
                                wy + halfWidth * cry + halfWidth * cuy,
                                wz + halfWidth * crz + halfWidth * cuz,
                                packedColor, 0, 0, OverlayTexture.NO_OVERLAY, light,
                                cux, cuy, cuz);
                        buffer.addVertex(
                                wx - halfWidth * crx + halfWidth * cux,
                                wy - halfWidth * cry + halfWidth * cuy,
                                wz - halfWidth * crz + halfWidth * cuz,
                                packedColor, 1, 0, OverlayTexture.NO_OVERLAY, light,
                                cux, cuy, cuz);
                    }
                }
            }
            data.lastTickParticleCount = particleCount;
            data.lastTickRenderCount = targetCount;
        });

        for (var veshEntry : data.getEntriesIterable()) {
            for (var stream : veshEntry.steamsIterable) {
                if (stream.attachedLogic.isEmpty()) continue;
                float elapsed = mc.level.getGameTime() - stream.receivedAtTick + partialTick;
                if (stream.isDying) elapsed = stream.deathAtTick - stream.receivedAtTick;
                Vec3 currentPos = stream.startPos.add(stream.motion.scale(stream.startTickCount + elapsed));

                double dx = currentPos.x - camera.pos.x;
                double dy = currentPos.y - camera.pos.y;
                double dz = currentPos.z - camera.pos.z;
                double distance = dx * dx + dy * dy + dz * dz;
                data.renderStamp(3);
                if (distance > 9000) continue;
                double dot = dx * fx + dy * fy + dz * fz;
                if (dot < -10.0) continue;


                if (!camera.cullFrustum.pointInFrustum(currentPos.x, currentPos.y, currentPos.z))
                    continue;

                for (var logic : stream.attachedLogic)
                    logic.onRender(stream, currentPos, camera, poseStack, collector);
            }
        }
    }

}
