package studio.fantasyit.ether_craft.particle.ether_stream;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import studio.fantasyit.ether_craft.EtherCraft;

@EventBusSubscriber
public class EtherStreamRenderPipeline {

    public static final RenderPipeline ETHER_RENDER_PIPELINE = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation(EtherCraft.id("pipeline/additive_particle_ether"))
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .build();
    @SubscribeEvent
    public static void register(RegisterRenderPipelinesEvent event){
        event.registerPipeline(ETHER_RENDER_PIPELINE);
    }
}
