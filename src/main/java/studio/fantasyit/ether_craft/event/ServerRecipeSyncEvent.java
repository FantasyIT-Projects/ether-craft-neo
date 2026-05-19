package studio.fantasyit.ether_craft.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import studio.fantasyit.ether_craft.factory.EtherProcessRecipeManager;
import studio.fantasyit.ether_craft.network.s2c.SyncExtraRecipesS2C;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;

@EventBusSubscriber
public class ServerRecipeSyncEvent {
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(RecipeTypeRegistry.ETHER_PROCESS_FACTORY_RECIPE.get());
        event.sendRecipes(RecipeTypeRegistry.NODE_PROCESS_RECIPE.get());
        EtherProcessRecipeManager.onReload(event.getPlayer().level().recipeAccess());
        PacketDistributor.sendToPlayer(
                (ServerPlayer) event.getPlayer(),
                new SyncExtraRecipesS2C(EtherProcessRecipeManager.extraRecipes)
        );
    }
}
