package studio.fantasyit.ether_craft.register;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.glass.render.EtherGlassUnbakedModel;
import studio.fantasyit.ether_craft.block.node.render.EtherAdaptNodeUnbakedModel;
import studio.fantasyit.ether_craft.block.node.render.EtherAdapterNodeBlockEntityRender;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class RenderModelRegister {
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                BlockEntityRegistry.ETHER_NODE_ENTITY.get(),
                EtherAdapterNodeBlockEntityRender::new
        );
    }
    @SubscribeEvent
    static void registerBlockStateModels(RegisterBlockStateModels event) {
        event.registerModel(EtherCraft.id("ether_glass"), EtherGlassUnbakedModel.CODEC);
        event.registerModel(EtherCraft.id("ether_adapt_node"), EtherAdaptNodeUnbakedModel.CODEC);
    }
}
