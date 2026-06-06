package studio.fantasyit.ether_craft.integration.sodium;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class SodiumIntegration {
    public static boolean pushVerticesImpl(VertexConsumer consumer, ByteBuffer data, int vertexCount) {
        VertexBufferWriter writer = VertexBufferWriter.tryOf(consumer);
        if (writer == null) return false;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VertexBufferWriter.copyInto(writer, stack,
                    MemoryUtil.memAddress(data),
                    vertexCount,
                    DefaultVertexFormat.ENTITY);
        }
        return true;
    }
}
