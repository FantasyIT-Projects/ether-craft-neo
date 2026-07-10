package studio.fantasyit.ether_craft.event;

import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import studio.fantasyit.ether_craft.EtherCraft;

import static studio.fantasyit.ether_craft.register.AttachmentDataRegistry.TAKEN_BY_ETHER_STREAM;

@EventBusSubscriber
public class PlayerRenderEvent {
    private static final ContextKey<Boolean> SHOULD_HIDE = new ContextKey<>(EtherCraft.id("stream_hide"));

    @SubscribeEvent
    public static void playerExtractRenderStateModifier(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T entity, AvatarRenderState renderState) {
                if (entity.hasData(TAKEN_BY_ETHER_STREAM))
                    renderState.setRenderData(SHOULD_HIDE, entity.getData(TAKEN_BY_ETHER_STREAM));
            }
        });
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre<?> event) {
        AvatarRenderState renderState = event.getRenderState();
        Boolean shouldHide = renderState.getRenderData(SHOULD_HIDE);
        if (shouldHide == null || !shouldHide) return;
        event.setCanceled(true);
    }
}
