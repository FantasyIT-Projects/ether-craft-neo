package studio.fantasyit.ether_craft.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;

@EventBusSubscriber
public class WorldRenderEvent {
    @SubscribeEvent
    public static void onWorldRender(ExtractBlockOutlineRenderStateEvent event) {

    }
}
