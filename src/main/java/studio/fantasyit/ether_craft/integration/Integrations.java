package studio.fantasyit.ether_craft.integration;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.fml.ModList;
import studio.fantasyit.ether_craft.integration.iris.IrisApiWrapper;
import studio.fantasyit.ether_craft.integration.sodium.SodiumIntegration;

import java.nio.ByteBuffer;

public class Integrations {

    public static boolean isSodiumLoaded() {
        return ModList.get().isLoaded("sodium");
    }

    public static boolean pushVertices(VertexConsumer consumer, ByteBuffer data, int vertexCount) {
        if (!isSodiumLoaded()) return false;
        return SodiumIntegration.pushVerticesImpl(consumer, data, vertexCount);
    }

    public static boolean isIrisLoaded() {
        return ModList.get().isLoaded("iris");
    }

    public static boolean hasIrisShaderPack() {
        if (!isIrisLoaded()) return false;
        return IrisApiWrapper.isIrisHasShaderLoaded();
    }
}
