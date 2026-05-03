//package studio.fantasyit.ether_craft.register;
//
//import net.minecraft.client.renderer.entity.EntityRenderers;
//import net.minecraft.core.registries.Registries;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.entity.MobCategory;
//import net.minecraftforge.eventbus.api.IEventBus;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//import net.minecraftforge.registries.DeferredRegister;
//import net.minecraftforge.registries.RegistryObject;
//import studio.fantasyit.ether_craft.EtherCraft;
//import studio.fantasyit.ether_craft.entity.EtherStreamEntity;
//import studio.fantasyit.ether_craft.entity.EtherStreamEntityRender;
//
//@Mod.EventBusSubscriber(modid = EtherCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
//public class EntityRegistry {
//
//    public static final DeferredRegister<EntityType<?>> ENTITY_TYPE_REGISTER =
//            DeferredRegister.create(Registries.ENTITY_TYPE, EtherCraft.MODID);
//    public static final RegistryObject<EntityType<EtherStreamEntity>> ETHER_STREAM_ENTITY = ENTITY_TYPE_REGISTER.register(
//            "ether_stream",
//            () -> EntityType.Builder.of(EtherStreamEntity::new, MobCategory.MISC)
//                    .setShouldReceiveVelocityUpdates(true)
//                    .setUpdateInterval(1)
//                    .setTrackingRange(128)
//                    .sized(.6F, .6F)
//                    .build("ether_stream")
//    );
//
//    public static void register(IEventBus eventBus) {
//        ENTITY_TYPE_REGISTER.register(eventBus);
//    }
//
//    @SubscribeEvent
//    public static void registerModel(FMLClientSetupEvent event){
//        EntityRenderers.register(ETHER_STREAM_ENTITY.get(), EtherStreamEntityRender::new);
//    }
//}
