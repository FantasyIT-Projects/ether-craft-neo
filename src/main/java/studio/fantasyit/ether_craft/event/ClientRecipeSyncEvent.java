package studio.fantasyit.ether_craft.event;

import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientRecipeSyncEvent {
    private static RecipeMap syncedRecipeMap = RecipeMap.EMPTY;

    @SubscribeEvent
    public static void onRecipesReceived(RecipesReceivedEvent event) {
        syncedRecipeMap = event.getRecipeMap();
    }

    public static RecipeMap getSyncedRecipeMap() {
        return syncedRecipeMap;
    }
}
