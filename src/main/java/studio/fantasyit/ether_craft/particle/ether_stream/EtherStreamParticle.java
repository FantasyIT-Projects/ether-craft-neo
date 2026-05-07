package studio.fantasyit.ether_craft.particle.ether_stream;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Quaternionf;

public class EtherStreamParticle extends SingleQuadParticle {
    int color;
    float initialSize = 0;
    Identifier loc;

    public EtherStreamParticle(ClientLevel level, double x, double y, double z, SpriteSet sprite, int color, float size) {
        super(level, x, y, z, sprite.first());
        this.lifetime = 6;
        this.quadSize = size;
        this.initialSize = size;
    }

    @Override
    public void tick() {
        this.setAlpha((float) (1 - (this.age / (this.lifetime + 0.1))));
        this.quadSize = (float) (this.quadSize / 1.5);
        super.tick();
    }

    public static final SingleQuadParticle.Layer LAYER = new SingleQuadParticle.Layer(
            true, TextureAtlas.LOCATION_PARTICLES, EtherStreamRenderPipeline.ETHER_RENDER_PIPELINE
    );

    @Override
    protected Layer getLayer() {
        return LAYER;
    }
}
