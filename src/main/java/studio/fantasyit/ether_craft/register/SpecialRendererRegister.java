package studio.fantasyit.ether_craft.register;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.item.renderer.AnswerItemOverlaySMR;

@EventBusSubscriber(value = Dist.CLIENT)
public class SpecialRendererRegister {
    public static final Identifier ANSWER_ITEM_OVERLAY = EtherCraft.id("answer_item_overlay");

    @SubscribeEvent
    public static void registerSpecialRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(
                ANSWER_ITEM_OVERLAY,
                AnswerItemOverlaySMR.Unbaked.MAP_CODEC
        );
    }
}
