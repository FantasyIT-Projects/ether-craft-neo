package studio.fantasyit.ether_craft.register;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.datapack.ProcessChipDataLoader;

@EventBusSubscriber
public class DataLoadRegister {
    @SubscribeEvent
    public static void onResourceReload(AddServerReloadListenersEvent event) {
        event.addListener(EtherCraft.id("process_chip_data"), new ProcessChipDataLoader());
    }
    @SubscribeEvent
    public static void onResourceReloadClient(AddClientReloadListenersEvent event) {
        event.addListener(EtherCraft.id("process_chip_data"), new ProcessChipDataLoader());
    }
}