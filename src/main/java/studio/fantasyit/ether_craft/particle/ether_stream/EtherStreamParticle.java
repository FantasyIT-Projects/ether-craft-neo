package studio.fantasyit.ether_craft.particle.ether_stream;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.resources.Identifier;

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


    @Override
    protected Layer getLayer() {
        return Layer.bySprite(this.sprite);
    }
}
