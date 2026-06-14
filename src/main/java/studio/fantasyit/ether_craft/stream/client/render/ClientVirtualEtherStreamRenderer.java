package studio.fantasyit.ether_craft.stream.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.integration.Integrations;
import studio.fantasyit.ether_craft.mixin.BufferBuilderAccessor;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHData;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHDataGetter;
import studio.fantasyit.ether_craft.stream.client.data.EntityStreamClientManager;

public class ClientVirtualEtherStreamRenderer {

    private static final Identifier TEXTURE = EtherCraft.id("textures/entity/ether_stream.png");
    private static final RenderType RENDER_TYPE = RenderTypes.energySwirl(TEXTURE, 0, 0);

    public static final VertexPrecomputer PRECOMPUTER = new VertexPrecomputer();

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

        double camX = camera.pos.x;
        double camY = camera.pos.y;
        double camZ = camera.pos.z;

        float camRightX = cameraRight.x;
        float camRightY = cameraRight.y;
        float camRightZ = cameraRight.z;

        float camUpX = cameraUp.x;
        float camUpY = cameraUp.y;
        float camUpZ = cameraUp.z;

        VertexPrecomputer.SnapshotData snapshot = RenderDataUtil.makeSnapShot(
                camX, camY, camZ,
                camRightX, camRightY, camRightZ,
                camUpX, camUpY, camUpZ,
                fx, fy, fz,
                camera.cullFrustum);
        PRECOMPUTER.submit(snapshot);

        collector.order(1).submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            data.startRenderStamp();
            VertexPrecomputer.PreComputedMesh mesh = PRECOMPUTER.tryTakeResult();

            if (mesh != null) {
                if (mesh.vertexCount() != 0) {
                    if (!Integrations.pushVertices(buffer, mesh.buffer(), mesh.vertexCount())) {
                        int totalBytes = mesh.vertexCount() * VertexPrecomputer.VERTEX_SIZE;
                        BufferBuilderAccessor accessor = (BufferBuilderAccessor) buffer;
                        long ptr = accessor.ether_craft$getBufferBuilder().reserve(totalBytes);
                        MemoryUtil.memCopy(
                                MemoryUtil.memAddress(mesh.buffer()),
                                ptr,
                                totalBytes
                        );
                        accessor.ether_craft$setVertexCount(accessor.ether_craft$getVertexCount() + mesh.vertexCount());
                    }
                }
                data.lastTickParticleCount = mesh.vertexCount() / 4;
                data.lastTickRenderCount = mesh.renderTargetCount();
            }
        });

        for (var veshEntry : data.getEntriesIterable()) {
            for (var stream : veshEntry.steamsIterable) {
                if (stream.attachedLogic.isEmpty()) continue;
                float elapsed = mc.level.getGameTime() - stream.receivedAtTick + partialTick;
                if (stream.isDying) elapsed = stream.deathAtTick - stream.receivedAtTick;
                Vec3 currentPos = stream.startPos.add(stream.motion.scale(stream.startTickCount + elapsed));

                double dx = currentPos.x - camX;
                double dy = currentPos.y - camY;
                double dz = currentPos.z - camZ;
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

        for (var stream : EntityStreamClientManager.entries) {
            if (stream.attachedLogic.isEmpty()) continue;
            float elapsed = mc.level.getGameTime() - stream.receivedAtTick + partialTick;
            if (stream.isDying) elapsed = stream.deathAtTick - stream.receivedAtTick;
            Vec3 currentPos = stream.startPos.add(stream.motion.scale(stream.startTickCount + elapsed));

            double dx = currentPos.x - camX;
            double dy = currentPos.y - camY;
            double dz = currentPos.z - camZ;
            double distance = dx * dx + dy * dy + dz * dz;
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
