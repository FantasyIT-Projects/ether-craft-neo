package studio.fantasyit.ether_craft.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.event.ClientRecipeSyncEvent;
import studio.fantasyit.ether_craft.factory.EtherProcessRecipeManager;
import studio.fantasyit.ether_craft.factory.ExtraRecipeProvider;
import studio.fantasyit.ether_craft.node.NodePluginManager;
import studio.fantasyit.ether_craft.recipe.crafting.UpgradeShapedRecipe;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.recipe.node.NodeProcessRecipe;
import studio.fantasyit.ether_craft.recipe.plating.PlatingRecipe;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
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
    public static final IRecipeType<NodeProcessRecipe> NODE_PROCESS_TYPE =
            IRecipeType.create(EtherCraft.MODID, "node_process", NodeProcessRecipe.class);
    public static final IRecipeType<PlatingRecipe> PLATING_TYPE =
            IRecipeType.create(EtherCraft.MODID, "plating", PlatingRecipe.class);

    private record DynamicCategory(
            IRecipeType<EtherProcessFactoryRecipe> type,
            Identifier categoryId
    ) {
    }

    private final List<DynamicCategory> dynamicCategories = new ArrayList<>();

    @Override
    public Identifier getPluginUid() {
        return EtherCraft.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();

        registration.addRecipeCategories(
                new EtherProcessCategory(guiHelper,
                        ETHER_PROCESS_TYPE,
                        Component.translatable("jei.ether_craft.ether_process"),
                        new ItemStack(ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_1.get())),
                new NodePluginInfoCategory(guiHelper),
                new NodeProcessCategory(guiHelper),
                new PlatingCategory(guiHelper)
        );

        for (ExtraRecipeProvider provider : EtherProcessRecipeManager.extraRecipeProviders) {
            Identifier catId = provider.getCategoryId();
            String ns = catId.getNamespace();
            String path = catId.getPath().replace('/', '.');
            String uid = "ether_process_" + path;
            IRecipeType<EtherProcessFactoryRecipe> type =
                    IRecipeType.create(EtherCraft.MODID, uid, EtherProcessFactoryRecipe.class);
            Component title = Component.translatable(
                    "jei.ether_craft.category." + ns + "." + catId.getPath().replace('/', '.'));
            EtherProcessCategory category = new EtherProcessCategory(guiHelper, type, title,
                    new ItemStack(provider.getIcon().asItem()));
            registration.addRecipeCategories(category);
            dynamicCategories.add(new DynamicCategory(type, catId));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var etherProcessRecipes = getEtherProcessRecipes();
        if (!etherProcessRecipes.isEmpty()) {
            registration.addRecipes(ETHER_PROCESS_TYPE, etherProcessRecipes);
        }
        var nodeProcessRecipes = getNodeProcessRecipes();
        if (!nodeProcessRecipes.isEmpty()) {
            registration.addRecipes(NODE_PROCESS_TYPE, nodeProcessRecipes);
        }
        registerNodePluginInfo(registration);

        var platingRecipes = getPlatingRecipes();
        if (!platingRecipes.isEmpty()) {
            registration.addRecipes(PLATING_TYPE, platingRecipes);
        }

        for (var dyn : dynamicCategories) {
            List<EtherProcessFactoryRecipe> catRecipes = EtherProcessRecipeManager.extraRecipes.stream()
                    .filter(e -> e.categoryId().equals(dyn.categoryId))
                    .map(EtherProcessRecipeManager.ExtraRecipe::recipe)
                    .toList();
            if (!catRecipes.isEmpty()) {
                registration.addRecipes(dyn.type, catRecipes);
            }
        }
    }

    private void registerNodePluginInfo(IRecipeRegistration registration) {
        List<NodePluginInfoRecipe> recipes = new ArrayList<>();
        for (var info : NodePluginManager.ALL_PLUGINS) {
            if (info.type() == NodePluginManager.PluginType.DUMMY || info.id().equals(NodePluginManager.MAIN_PAGE.pluginId()))
                continue;
            recipes.add(NodePluginInfoRecipe.fromPluginInfo(info));
        }
        registration.addRecipes(NODE_PLUGIN_INFO_TYPE, recipes);
    }

    private static List<EtherProcessFactoryRecipe> getEtherProcessRecipes() {
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

    private static List<PlatingRecipe> getPlatingRecipes() {
        List<PlatingRecipe> result = new ArrayList<>();
        var syncedMap = ClientRecipeSyncEvent.getSyncedRecipeMap();
        if (syncedMap != null) {
            for (RecipeHolder<PlatingRecipe> holder : syncedMap.byType(RecipeTypeRegistry.PLATING_RECIPE.get())) {
                result.add(holder.value());
            }
        }

        if (!result.isEmpty()) {
            return result;
        }

        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
                if (holder.value() instanceof PlatingRecipe recipe) {
                    result.add(recipe);
                }
            }
        }

        return result;
    }

    private static List<NodeProcessRecipe> getNodeProcessRecipes() {
        List<NodeProcessRecipe> result = new ArrayList<>();
        var syncedMap = ClientRecipeSyncEvent.getSyncedRecipeMap();
        if (syncedMap != null) {
            for (RecipeHolder<NodeProcessRecipe> holder : syncedMap.byType(RecipeTypeRegistry.NODE_PROCESS_RECIPE.get())) {
                result.add(holder.value());
            }
        }

        if (!result.isEmpty()) {
            return result;
        }

        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
                if (holder.value() instanceof NodeProcessRecipe recipe) {
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
        registration.addCraftingStation(NODE_PROCESS_TYPE,
                new ItemStack(ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_1.get()),
                new ItemStack(ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_2.get()),
                new ItemStack(ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_3.get())
        );
        registration.addCraftingStation(PLATING_TYPE,
                new ItemStack(ItemRegistry.ETHER_STREAM_EMITTER_ITEM.get()),
                new ItemStack(ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_1.get()),
                new ItemStack(ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_2.get()),
                new ItemStack(ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_3.get())
        );

        for (var dyn : dynamicCategories) {
            registration.addCraftingStation(dyn.type,
                    new ItemStack(ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_1.get()),
                    new ItemStack(ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_2.get()),
                    new ItemStack(ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_3.get()),
                    new ItemStack(ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_4.get())
            );
        }
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerFromDataComponentTypes(
                ItemRegistry.PROCESS_CHIP_ITEM.get(),
                DataComponentRegistry.CHIP_ID.get()
        );
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(
                UpgradeShapedRecipe.class,
                new UpgradeShapedRecipeExtension()
        );
    }
}
