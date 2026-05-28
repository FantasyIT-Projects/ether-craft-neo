package studio.fantasyit.ether_craft.register;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.entity.stream.EtherStreamEntity;
import studio.fantasyit.ether_craft.entity.stream.render.EtherStreamEntityRenderer;

@EventBusSubscriber(modid = EtherCraft.MODID)
public class EntityRegistry {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPE_REGISTER =
            DeferredRegister.create(Registries.ENTITY_TYPE, EtherCraft.MODID);
    public static final DeferredHolder<EntityType<?>, @org.jetbrains.annotations.NotNull EntityType<EtherStreamEntity>> ETHER_STREAM_ENTITY = ENTITY_TYPE_REGISTER.register(
            "ether_stream",
            (l) -> EntityType.Builder.of((EntityType.EntityFactory<EtherStreamEntity>) EtherStreamEntity::new, MobCategory.MISC)
                    .setShouldReceiveVelocityUpdates(true)
                    .setUpdateInterval(1)
                    .setTrackingRange(128)
                    .sized(.6F, .6F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, l))
    );

    public static void register(IEventBus eventBus) {
        ENTITY_TYPE_REGISTER.register(eventBus);
    }

    @SubscribeEvent
    public static void registerModel(FMLClientSetupEvent event) {
        EntityRenderers.register(ETHER_STREAM_ENTITY.get(), EtherStreamEntityRenderer::new);
    }
}
