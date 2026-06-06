package studio.fantasyit.ether_craft.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessor {

    @Accessor("buffer")
    ByteBufferBuilder ether_craft$getBufferBuilder();

    @Accessor("vertices")
    int ether_craft$getVertexCount();

    @Accessor("vertices")
    void ether_craft$setVertexCount(int count);
}
