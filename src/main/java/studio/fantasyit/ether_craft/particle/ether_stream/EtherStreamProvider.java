package studio.fantasyit.ether_craft.particle.ether_stream;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public class EtherStreamProvider implements ParticleProvider<EtherStreamData> {
    private final SpriteSet spriteSet;

    public EtherStreamProvider(SpriteSet spriteSet) {
        this.spriteSet = spriteSet;
    }

    @Override
    public @Nullable Particle createParticle(EtherStreamData type, ClientLevel clientLevel, double x, double y, double z, double v3, double v4, double v5, RandomSource randomSource) {
        return new EtherStreamParticle(clientLevel, x, y, z,this.spriteSet,type.color, type.size);
    }
}
