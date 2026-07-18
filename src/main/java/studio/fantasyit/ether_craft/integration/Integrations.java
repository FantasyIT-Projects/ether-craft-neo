package studio.fantasyit.ether_craft.integration;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import studio.fantasyit.ether_craft.integration.iris.IrisApiWrapper;
import studio.fantasyit.ether_craft.integration.sodium.SodiumIntegration;

import java.nio.ByteBuffer;

public class Integrations {

    public static boolean isGuideMeLoaded() {
        return ModList.get().isLoaded("guideme");
    }

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

    public static boolean hasPowerTool() {
        return ModList.get().isLoaded("powertool");
    }

    public static boolean hasPowerToolLoading() {
        ModFileInfo pt =  FMLLoader.getCurrent().getLoadingModList().getModFileById("powertool");
        return (pt != null);
    }
}
