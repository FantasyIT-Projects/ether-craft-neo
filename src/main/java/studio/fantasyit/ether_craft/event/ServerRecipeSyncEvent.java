package studio.fantasyit.ether_craft.event;

import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import studio.fantasyit.ether_craft.factory.EtherProcessChipManager;
import studio.fantasyit.ether_craft.factory.EtherProcessRecipeManager;
import studio.fantasyit.ether_craft.network.s2c.SyncChipInfoS2C;
import studio.fantasyit.ether_craft.network.s2c.SyncExtraRecipesS2C;
import studio.fantasyit.ether_craft.network.s2c.SyncPluginTipsS2C;
import studio.fantasyit.ether_craft.node.tip.NodePluginTipManager;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;

@EventBusSubscriber
public class ServerRecipeSyncEvent {
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(RecipeTypeRegistry.ETHER_PROCESS_FACTORY_RECIPE.get());
        event.sendRecipes(RecipeTypeRegistry.NODE_PROCESS_RECIPE.get());
        RecipeManager recipeManager = event.getPlayerList().getServer().getRecipeManager();
        EtherProcessRecipeManager.onReload(recipeManager);
        NodePluginTipManager.INSTANCE.collect(recipeManager);
        event.getRelevantPlayers().forEach(player -> {
            PacketDistributor.sendToPlayer(player,
                    new SyncExtraRecipesS2C(EtherProcessRecipeManager.extraRecipes)
            );
            PacketDistributor.sendToPlayer(player,
                    new SyncChipInfoS2C(EtherProcessChipManager.chipInfo)
            );
            PacketDistributor.sendToPlayer(player,
                    new SyncPluginTipsS2C(
                            NodePluginTipManager.INSTANCE.getAllTips().entrySet().stream()
                                    .map(e -> new SyncPluginTipsS2C.Entry(e.getKey(), e.getValue()))
                                    .toList()
                    )
            );
        });
    }
}
