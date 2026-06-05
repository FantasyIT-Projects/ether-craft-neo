package studio.fantasyit.ether_craft.stream.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.client.data.ClientStreamEntry;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHData;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHDataGetter;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHEntry;
import studio.fantasyit.ether_craft.stream.client.extra.EtherStreamClientLogicManager;

public class ClientVirtualEtherStreamRenderer {

    private static final Identifier TEXTURE = EtherCraft.id("textures/particle/ether_stream.png");
    private static final RenderType RENDER_TYPE = RenderType.create(
            "ether_stream_tail_custom",
            RenderSetup.builder(EtherStreamRenderPipeline.ETHER_STREAM_ENTITY_PIPELINE)
                    .withTexture("Sampler0", TEXTURE)
                    .sortOnUpload()
                    .createRenderSetup()
    );

    public static void onRender(Minecraft mc, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        ClientVESHData data = ClientVESHDataGetter.get();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Quaternionf invOrientation = camera.orientation.conjugate(new Quaternionf());
        Vector3f offsetVec = new Vector3f();

        poseStack.pushPose();
        poseStack.mulPose(camera.orientation);

        Vector3f cameraForward = new Vector3f(0, 0, -1);
        camera.orientation.transform(cameraForward);
        float fx = cameraForward.x(), fy = cameraForward.y(), fz = cameraForward.z();
        data.startRenderStamp();

        collector.order(1).submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            int targetCount = 0;
            int particleCount = 0;
            float[] sizeFactor = new float[6];
            for (var i = 0; i < 6; i++) sizeFactor[i] = (float) Math.pow(1.5, i);

            Vector3f normal = pose.transformNormal(0, 1f, 0, new Vector3f());
            for (ObjectIterator<Object2ObjectMap.Entry<PosDir, ClientVESHEntry>> it = data.getEntries().object2ObjectEntrySet().fastIterator(); it.hasNext(); ) {
                var posEntry = it.next();
                ClientVESHEntry veshEntry = posEntry.getValue();
                for (ObjectIterator<Object2ObjectMap.Entry<Integer, ClientStreamEntry>> iter = veshEntry.streams.object2ObjectEntrySet().fastIterator(); iter.hasNext(); ) {
                    var streamEntry = iter.next();
                    data.renderStamp(0);
                    ClientStreamEntry stream = streamEntry.getValue();
                    if (stream.isDying() || !stream.shouldRender)
                        continue;
                    data.renderStamp(5);

                    long elapsed = mc.level.getGameTime() - stream.receivedAtTick;
                    Vec3 currentPos = stream.startPos.add(
                            stream.motion.scale(stream.startTickCount + elapsed));//这里不加pt；
                    double dx = currentPos.x - camera.pos.x;
                    double dy = currentPos.y - camera.pos.y;
                    double dz = currentPos.z - camera.pos.z;
                    double distance = dx * dx + dy * dy + dz * dz;

                    double dot = dx * fx + dy * fy + dz * fz;
                    if (dot < -10.0) continue;

                    if (distance > 9000) {
                        if (streamEntry.getKey() % 4 != 0) continue;
                    } else if (distance > 1600) {
                        if (streamEntry.getKey() % 2 != 0) continue;
                    }
                    data.renderStamp(1);

                    Vec3 baseStep = stream.motion.reverse();
                    Vec3 tailEnd = currentPos.add(baseStep.scale(5));
                    if (!camera.cullFrustum.pointInFrustum(currentPos.x, currentPos.y, currentPos.z)
                            && !camera.cullFrustum.pointInFrustum(tailEnd.x, tailEnd.y, tailEnd.z))
                        continue;
                    data.renderStamp(2);
                    targetCount++;
                    for (int i = 0; i < 6; i++) {
                        data.renderStamp(3);
                        Vec3 tailPos = currentPos.add(baseStep.scale(i));
                        if (i != 0 && tailPos.distanceToSqr(camera.pos) > 225 * (5 - i)) {
                            data.renderStamp(4);
                            break;
                        }
                        particleCount++;
                        offsetVec.set(
                                (float) (tailPos.x - camera.pos.x),
                                (float) (tailPos.y - camera.pos.y),
                                (float) (tailPos.z - camera.pos.z));
                        invOrientation.transform(offsetVec);

                        float alpha = 1f - (float) i / 6.1f;
                        float szFact = (float) (0.03f * Math.log10(stream.ether));

                        float halfWidth = szFact * 0.5f / sizeFactor[i];
                        int a = (int) (alpha * 255);
                        int light = 0xF000F0;
                        data.renderStamp(4);
                        vertex(buffer, pose, -halfWidth + offsetVec.x, -halfWidth + offsetVec.y, offsetVec.z, a, 1, 1, light, normal);
                        vertex(buffer, pose, halfWidth + offsetVec.x, -halfWidth + offsetVec.y, offsetVec.z, a, 0, 1, light, normal);
                        vertex(buffer, pose, halfWidth + offsetVec.x, halfWidth + offsetVec.y, offsetVec.z, a, 0, 0, light, normal);
                        vertex(buffer, pose, -halfWidth + offsetVec.x, halfWidth + offsetVec.y, offsetVec.z, a, 1, 0, light, normal);
                        data.renderStamp(9);
                    }
                }
            }
            data.lastTickParticleCount = particleCount;
            data.lastTickRenderCount = targetCount;
        });

        poseStack.popPose();

        for (var posEntry : data.getEntries().entrySet()) {
            ClientVESHEntry veshEntry = posEntry.getValue();
            for (var streamEntry : veshEntry.streams.entrySet()) {
                ClientStreamEntry stream = streamEntry.getValue();
                if (stream.isRemoved()) continue;

                float elapsed = mc.level.getGameTime() - stream.receivedAtTick + partialTick;
                if (stream.isDying) elapsed = stream.deathAtTick - stream.receivedAtTick;
                Vec3 currentPos = stream.startPos.add(
                        stream.motion.scale(stream.startTickCount + elapsed));

                double dx = currentPos.x - camera.pos.x;
                double dy = currentPos.y - camera.pos.y;
                double dz = currentPos.z - camera.pos.z;
                double distance = dx * dx + dy * dy + dz * dz;

                double dot = dx * fx + dy * fy + dz * fz;
                if (dot < -10.0) continue;

                if (distance > 9000) {
                    continue;
                }

                double speed = stream.motion.length();
                if (speed > 0.0001) {
                    Vec3 tailEnd = currentPos.add(stream.motion.reverse().scale(5));
                    if (!camera.cullFrustum.pointInFrustum(currentPos.x, currentPos.y, currentPos.z)
                            && !camera.cullFrustum.pointInFrustum(tailEnd.x, tailEnd.y, tailEnd.z))
                        continue;
                }


                EtherStreamClientLogicManager.renderExtra(stream, currentPos, camera, poseStack, collector);
            }
        }
    }

    public static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, int a, float u, float v, int light, Vector3f norm) {
        buffer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(norm.x, norm.y, norm.z);
    }
}
