package studio.fantasyit.ether_craft.stream.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

public class VertexPrecomputer {

    static final int VERTEX_SIZE = 36;
    private static final int[] ALPHAS = {255, 213, 170, 128, 85, 42};
    private static final float[] SIZE_FACTORS = new float[6];

    static {
        for (int i = 0; i < 6; i++) {
            SIZE_FACTORS[i] = (float) (0.5f / Math.pow(1.5, i));
        }
    }

    private final Thread worker;
    private volatile boolean hasWork;
    private volatile boolean running = true;
    private volatile SnapshotData snapshot;
    private volatile List<EntrySnapshot> entries;
    public volatile PreComputedMesh result = null;

    public VertexPrecomputer() {
        this.worker = Thread.ofPlatform()
                .name("EtherStream-Precompute")
                .daemon(true)
                .start(this::runLoop);
    }

    public void submit(SnapshotData data) {
        this.snapshot = data;
        this.hasWork = true;
    }

    public void submitEntries(List<EntrySnapshot> entries) {
        this.entries = entries;
        this.hasWork = true;
    }

    public PreComputedMesh tryTakeResult() {
        return result;
    }

    public void shutdown() {
        running = false;
        worker.interrupt();
    }

    private void runLoop() {
        while (running) {
            if (!hasWork) {
                LockSupport.parkNanos(100_000);
                continue;
            }
            SnapshotData data = this.snapshot;
            List<EntrySnapshot> entries = this.entries;
            this.snapshot = null;
            this.hasWork = false;
            if (data == null || entries == null) continue;
            this.result = compute(entries, data);
        }
    }

    private PreComputedMesh compute(List<EntrySnapshot> entries, SnapshotData data) {
        int totalSz = 0;
        for (var e : entries)
            totalSz += e.streams.size();
        int bufferSize = totalSz * 24 * VERTEX_SIZE;
        ByteBuffer buf = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.LITTLE_ENDIAN);
        ByteBufferVertexConsumer writer = new ByteBufferVertexConsumer(buf);
        RenderStats stats = renderStreams(writer, entries, data);
        buf.flip();

        return new PreComputedMesh(buf, stats.vertexCount, stats.renderTargetCount);
    }

    public static RenderStats renderStreams(VertexConsumer consumer, List<EntrySnapshot> entries, SnapshotData data) {
        int vertexCount = 0;
        int renderTargetCount = 0;

        for (var entry : entries) {
            for (var stream : entry.streams) {
                if (stream.isDying || !stream.shouldRender) continue;

                double dx = stream.currentPos.x - data.camX;
                double dy = stream.currentPos.y - data.camY;
                double dz = stream.currentPos.z - data.camZ;
                double distance = dx * dx + dy * dy + dz * dz;
                double dot = dx * data.fx + dy * data.fy + dz * data.fz;

                if (dot < -10.0) continue;

                if (distance > 9000) {
                    if (stream.id % 4 != 0) continue;
                } else if (distance > 1600) {
                    if (stream.id % 2 != 0) continue;
                }

                Vec3 tailEnd = stream.currentPos.add(stream.reverseStepMotions[5]);
                if (!data.cullFrustum.pointInFrustum(stream.currentPos.x, stream.currentPos.y, stream.currentPos.z)
                        && !data.cullFrustum.pointInFrustum(tailEnd.x, tailEnd.y, tailEnd.z)) {
                    continue;
                }

                renderTargetCount++;
                float szFact = (float) (0.03f * Math.log10(stream.ether));

                for (int i = 0; i < 6; i++) {
                    if (i != 0 && distance > 225 * (5 - i)) break;

                    Vec3 tailPos = stream.currentPos.add(stream.reverseStepMotions[i]);
                    float wx = (float) (tailPos.x - data.camX);
                    float wy = (float) (tailPos.y - data.camY);
                    float wz = (float) (tailPos.z - data.camZ);

                    float halfWidth = szFact * SIZE_FACTORS[i];
                    int light = 0xF000F0;
                    int packedColor = ARGB.color(ALPHAS[i], 255, 255, 255);

                    float crx = data.crx, cry = data.cry, crz = data.crz;
                    float cux = data.cux, cuy = data.cuy, cuz = data.cuz;

                    if (consumer != null) {
                        consumer.addVertex(
                                wx - halfWidth * crx - halfWidth * cux,
                                wy - halfWidth * cry - halfWidth * cuy,
                                wz - halfWidth * crz - halfWidth * cuz,
                                packedColor, 1, 1, 0, light,
                                cux, cuy, cuz);
                        consumer.addVertex(
                                wx + halfWidth * crx - halfWidth * cux,
                                wy + halfWidth * cry - halfWidth * cuy,
                                wz + halfWidth * crz - halfWidth * cuz,
                                packedColor, 0, 1, 0, light,
                                cux, cuy, cuz);
                        consumer.addVertex(
                                wx + halfWidth * crx + halfWidth * cux,
                                wy + halfWidth * cry + halfWidth * cuy,
                                wz + halfWidth * crz + halfWidth * cuz,
                                packedColor, 0, 0, 0, light,
                                cux, cuy, cuz);
                        consumer.addVertex(
                                wx - halfWidth * crx + halfWidth * cux,
                                wy - halfWidth * cry + halfWidth * cuy,
                                wz - halfWidth * crz + halfWidth * cuz,
                                packedColor, 1, 0, 0, light,
                                cux, cuy, cuz);
                    }
                    vertexCount += 4;
                }
            }
        }

        return new RenderStats(vertexCount, renderTargetCount);
    }

    private record ByteBufferVertexConsumer(ByteBuffer buf) implements VertexConsumer {

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v,
                              int overlay, int light, float nx, float ny, float nz) {
            buf.putFloat(x);
            buf.putFloat(y);
            buf.putFloat(z);
            buf.putInt(ARGB.toABGR(color));
            buf.putFloat(u);
            buf.putFloat(v);
            buf.putInt(overlay);
            buf.putInt(light);
            buf.put(normalByte(nx));
            buf.put(normalByte(ny));
            buf.put(normalByte(nz));
            buf.put((byte) 0);
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setOverlay(int packedOverlay) {
            return this;
        }

        @Override
        public VertexConsumer setLight(int packedLight) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }

        private static byte normalByte(float c) {
            return (byte) ((int) (Mth.clamp(c, -1.0F, 1.0F) * 127.0F) & 255);
        }
    }

    public record RenderStats(int vertexCount, int renderTargetCount) {
    }

    public record StreamSnapshot(
            Vec3 currentPos,
            Vec3[] reverseStepMotions,
            int id,
            int ether,
            boolean isDying,
            boolean shouldRender
    ) {
    }

    public record EntrySnapshot(List<StreamSnapshot> streams) {
    }

    public record SnapshotData(
            double camX, double camY, double camZ,
            float crx, float cry, float crz,
            float cux, float cuy, float cuz,
            float fx, float fy, float fz,
            Frustum cullFrustum
    ) {
    }

    public record PreComputedMesh(
            ByteBuffer buffer,
            int vertexCount,
            int renderTargetCount
    ) {
    }
}
