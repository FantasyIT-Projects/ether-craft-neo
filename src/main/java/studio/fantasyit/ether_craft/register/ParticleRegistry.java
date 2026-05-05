package studio.fantasyit.ether_craft.register;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.particle.ether_stream.EtherStreamProvider;
import studio.fantasyit.ether_craft.particle.ether_stream.EtherStreamType;

import java.util.function.Supplier;

@EventBusSubscriber(modid = EtherCraft.MODID)
public class ParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, EtherCraft.MODID);

    public static final Supplier<EtherStreamType> ETHER_STREAM_PARTICLE = PARTICLE_TYPES.register(
            "ether_stream",
            () -> new EtherStreamType(false)
    );
    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }

    @SubscribeEvent
    public static void registerProvider(RegisterParticleProvidersEvent event){
        event.registerSpriteSet(ETHER_STREAM_PARTICLE.get(), EtherStreamProvider::new);
    }
}
