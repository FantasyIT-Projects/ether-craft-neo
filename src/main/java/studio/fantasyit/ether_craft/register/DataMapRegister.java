package studio.fantasyit.ether_craft.register;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import studio.fantasyit.ether_craft.datapack.StoneGeneratorRatio;

@EventBusSubscriber
public class DataMapRegister {
    @SubscribeEvent // on the mod event bus
    public static void registerDataMapTypes(RegisterDataMapTypesEvent event) {
        event.register(StoneGeneratorRatio.STONE_GENERATOR_RATIO);
    }
}
