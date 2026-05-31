package studio.fantasyit.ether_craft.stream.client.render;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class RenderPipelineRegister {
    @SubscribeEvent
    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(EtherStreamRenderPipeline.ETHER_RENDER_PIPELINE);
        event.registerPipeline(EtherStreamRenderPipeline.ETHER_STREAM_ENTITY_PIPELINE);
    }
}
