package studio.fantasyit.ether_craft.register;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.glass.render.EtherGlassModel;
import studio.fantasyit.ether_craft.block.node.render.EtherAdapterNodeBlockEntityRender;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class ClientRenderRegister {
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                BlockEntityRegistry.ETHER_NODE_ENTITY.get(),
                EtherAdapterNodeBlockEntityRender::new
        );
    }

    @SubscribeEvent
    public static void registerBlockStateModels(RegisterBlockStateModels event) {
        event.registerModel(EtherGlassModel.Unbaked.ID, EtherGlassModel.Unbaked.MAP_CODEC);
    }
}
