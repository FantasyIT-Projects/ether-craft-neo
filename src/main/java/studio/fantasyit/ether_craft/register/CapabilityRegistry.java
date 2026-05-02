package studio.fantasyit.ether_craft.register;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.capability.EtherContainer;

@EventBusSubscriber(modid = EtherCraft.MODID)
public class CapabilityRegistry {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                EtherContainer.ITEM_HANDLER_BLOCK, // capability to register for
                (level, pos, state, be, side) -> new EtherContainer(be)
    );
    }
}
