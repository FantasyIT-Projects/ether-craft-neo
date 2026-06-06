package studio.fantasyit.ether_craft.integration;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.fml.ModList;
import studio.fantasyit.ether_craft.integration.sodium.SodiumIntegration;

import java.nio.ByteBuffer;

public class Integrations {
    private static final boolean SODIUM_LOADED = ModList.get().isLoaded("sodium");

    public static boolean isSodiumLoaded() {
        return SODIUM_LOADED;
    }

    public static boolean pushVertices(VertexConsumer consumer, ByteBuffer data, int vertexCount) {
        if (!SODIUM_LOADED) return false;
        return SodiumIntegration.pushVerticesImpl(consumer, data, vertexCount);
    }
}
