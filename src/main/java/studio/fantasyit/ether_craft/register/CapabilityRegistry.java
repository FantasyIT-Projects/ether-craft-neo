package studio.fantasyit.ether_craft.register;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.base.EtherContainer;

@EventBusSubscriber(modid = EtherCraft.MODID)
public class CapabilityRegistry {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                EtherContainer.ETHER_CONTAINER,
                (level, pos, state, be, side) -> (EtherContainer) be,
                BlockRegistry.ETHER_PROCESS_FACTORY.get()
        );
    }
}
