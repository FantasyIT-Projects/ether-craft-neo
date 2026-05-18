package studio.fantasyit.ether_craft.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;

@EventBusSubscriber
public class ServerRecipeSyncEvent {
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(RecipeTypeRegistry.ETHER_PROCESS_FACTORY_RECIPE.get());
        event.sendRecipes(RecipeTypeRegistry.NODE_PROCESS_RECIPE.get());
    }
}
