package studio.fantasyit.ether_craft.register;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.menu.grid.answer.AnswerFetchScreen;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryScreen;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class ClientGuiRegistry {
    @SubscribeEvent
    public static void init(RegisterMenuScreensEvent event) {
        event.register(GuiRegistry.ETHER_PROCESS_FACTORY_CONTAINER.get(), EtherProcessFactoryScreen::new);
        event.register(GuiRegistry.ETHER_ADAPT_NODE_CONTAINER.get(), EtherAdaptNodeScreen::new);
        event.register(GuiRegistry.ANSWER_FETCH.get(), AnswerFetchScreen::new);
    }

}
