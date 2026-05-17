package studio.fantasyit.ether_craft.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.event.ClientRecipeSyncEvent;
import studio.fantasyit.ether_craft.node.NodePluginManager;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.register.ItemRegistry;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
    public static final IRecipeType<EtherProcessFactoryRecipe> ETHER_PROCESS_TYPE =
            IRecipeType.create(EtherCraft.MODID, "ether_process", EtherProcessFactoryRecipe.class);
    public static final IRecipeType<NodePluginInfoRecipe> NODE_PLUGIN_INFO_TYPE =
            IRecipeType.create(EtherCraft.MODID, "node_plugin_info", NodePluginInfoRecipe.class);

    @Override
    public Identifier getPluginUid() {
        return EtherCraft.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new EtherProcessCategory(guiHelper),
                new NodePluginInfoCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var recipes = getRecipes();
        if (!recipes.isEmpty()) {
            registration.addRecipes(ETHER_PROCESS_TYPE, recipes);
        }
        registerNodePluginInfo(registration);
    }

    private void registerNodePluginInfo(IRecipeRegistration registration) {
        List<NodePluginInfoRecipe> recipes = new ArrayList<>();
        for (var info : NodePluginManager.ALL_PLUGINS) {
            if (info.type() == NodePluginManager.PluginType.DUMMY)
                continue;
            recipes.add(NodePluginInfoRecipe.fromPluginInfo(info));
        }
        registration.addRecipes(NODE_PLUGIN_INFO_TYPE, recipes);
    }

    private static List<EtherProcessFactoryRecipe> getRecipes() {
        List<EtherProcessFactoryRecipe> result = new ArrayList<>();

        var syncedMap = ClientRecipeSyncEvent.getSyncedRecipeMap();
        if (syncedMap != null) {
            for (RecipeHolder<EtherProcessFactoryRecipe> holder : syncedMap.byType(RecipeTypeRegistry.ETHER_PROCESS_FACTORY_RECIPE.get())) {
                result.add(holder.value());
            }
        }

        if (!result.isEmpty()) {
            return result;
        }

        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
                if (holder.value() instanceof EtherProcessFactoryRecipe recipe) {
                    result.add(recipe);
                }
            }
        }

        return result;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(ETHER_PROCESS_TYPE,
                new ItemStack(ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_1.get()),
                new ItemStack(ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_2.get()),
                new ItemStack(ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_3.get()),
                new ItemStack(ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_4.get())
        );
        registration.addCraftingStation(NODE_PLUGIN_INFO_TYPE,
                new ItemStack(ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_1.get()),
                new ItemStack(ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_2.get()),
                new ItemStack(ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_3.get())
        );
    }
}
