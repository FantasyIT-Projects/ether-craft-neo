package studio.fantasyit.ether_craft.stream.client.render;

import studio.fantasyit.ether_craft.stream.client.data.ClientStreamEntry;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHData;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHEntry;

import java.util.ArrayList;
import java.util.List;

public class RenderDataUtil {

    public static List<VertexPrecomputer.EntrySnapshot> buildEntries(ClientVESHData data) {
        List<ClientVESHEntry> entries = data.getEntriesIterable();
        List<VertexPrecomputer.EntrySnapshot> entrySnapshots = new ArrayList<>(entries.size());

        for (var veshEntry : entries) {
            List<ClientStreamEntry> streams = veshEntry.steamsIterable;
            List<VertexPrecomputer.StreamSnapshot> streamSnapshots = new ArrayList<>(streams.size());

            for (var stream : streams) {
                streamSnapshots.add(new VertexPrecomputer.StreamSnapshot(
                        stream.currentPos,
                        stream.reverseStepMotions,
                        stream.id,
                        stream.ether,
                        stream.isDying,
                        stream.shouldRender
                ));
            }

            entrySnapshots.add(new VertexPrecomputer.EntrySnapshot(streamSnapshots));
        }

        return entrySnapshots;
    }

    public static VertexPrecomputer.SnapshotData makeSnapShot(
            double camX, double camY, double camZ,
            float crx, float cry, float crz,
            float cux, float cuy, float cuz,
            float fx, float fy, float fz,
            net.minecraft.client.renderer.culling.Frustum cullFrustum) {
        return new VertexPrecomputer.SnapshotData(
                camX, camY, camZ,
                crx, cry, crz,
                cux, cuy, cuz,
                fx, fy, fz,
                cullFrustum
        );
    }
}
