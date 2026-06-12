package studio.fantasyit.ether_craft.node.tip;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.common.crafting.DifferenceIngredient;
import studio.fantasyit.ether_craft.node.plugins.feature.*;
import studio.fantasyit.ether_craft.node.plugins.function.*;
import studio.fantasyit.ether_craft.node.plugins.upgrade.*;
import studio.fantasyit.ether_craft.recipe.node.NodeProcessRecipe;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.*;

public class NodePluginTipManager {
    public static final NodePluginTipManager INSTANCE = new NodePluginTipManager();

    private final Map<Identifier, TipInfo> tips = new HashMap<>();

    public void collect(RecipeManager recipeManager) {
        tips.clear();

        registerTip(FunctionFurnaceGenerator.ID,
                new TipInfo(List.of(Ingredient.of(Items.FURNACE)), List.of(), Set.of(TipConcept.ETHER_PRODUCTION)));
        registerTip(FunctionFurnaceGenerator.ID_BLAST,
                new TipInfo(List.of(Ingredient.of(Items.BLAST_FURNACE)), List.of(), Set.of(TipConcept.ETHER_PRODUCTION)));
        registerTip(FunctionStoneGenerator.ID,
                new TipInfo(List.of(Ingredient.of(Items.STONECUTTER)), List.of(), Set.of(TipConcept.ETHER_PRODUCTION)));
        registerTip(FunctionMagnet.ID,
                new TipInfo(List.of(Ingredient.of(ItemRegistry.VACUUM_PIPE.get())), List.of(), Set.of(TipConcept.LOGISTICS)));
        registerTip(FunctionEquipmentConsumeGenerator.ID,
                new TipInfo(List.of(Ingredient.of(Items.GRINDSTONE)), List.of(), Set.of(TipConcept.ETHER_PRODUCTION)));
        registerTip(FunctionEtherConverter.ID,
                new TipInfo(List.of(Ingredient.of(Items.DRAGON_EGG)), List.of(), Set.of(TipConcept.ETHER_PRODUCTION)));
        registerTip(FunctionGrowthAccelerator.ID,
                new TipInfo(List.of(Ingredient.of(Items.BONE_MEAL)), List.of(), Set.of(TipConcept.WORLD_INTERACTION)));
        registerTip(FunctionEnchanter.ID,
                new TipInfo(List.of(Ingredient.of(Items.ENCHANTING_TABLE)), List.of(), Set.of(TipConcept.CRAFTING)));
        registerTip(FunctionCreativeEther.ID,
                new TipInfo(List.of(Ingredient.of(ItemRegistry.ETHER_CREATIVE.get())), List.of(), Set.of(TipConcept.ETHER_PRODUCTION)));
        registerTip(FunctionCreativeEther.ID_FUNC,
                new TipInfo(List.of(Ingredient.of(ItemRegistry.ETHER_CREATIVE.get())), List.of(), Set.of(TipConcept.ETHER_PRODUCTION)));

        registerTip(FeatureEtherStreamEmitter.ID,
                new TipInfo(List.of(Ingredient.of(Items.DISPENSER)), List.of(), Set.of(TipConcept.ETHER_FLOW, TipConcept.LOGISTICS)));
        registerTip(FeatureDropperThrower.ID,
                new TipInfo(List.of(Ingredient.of(Items.DROPPER)), List.of(), Set.of(TipConcept.LOGISTICS, TipConcept.WORLD_INTERACTION)));
        registerTip(FeatureContainerInteract.ID,
                new TipInfo(List.of(Ingredient.of(Items.HOPPER)), List.of(), Set.of(TipConcept.LOGISTICS)));
        registerTip(FeatureRedstoneSignal.ID,
                new TipInfo(List.of(Ingredient.of(Items.COMPARATOR)), List.of(), Set.of(TipConcept.LOGISTICS)));
        registerTip(RedstoneSwitchUpgrade.ID,
                new TipInfo(List.of(Ingredient.of(Items.REDSTONE)), List.of(), Set.of(TipConcept.LOGISTICS)));
        registerTip(DestructionUpgrade.ID,
                new TipInfo(List.of(Ingredient.of(Items.LAVA_BUCKET)), List.of(), Set.of(TipConcept.LOGISTICS)));

        registerTip(EtherStorageUpgrade.ID,
                new TipInfo(List.of(Ingredient.of(ItemRegistry.ETHERPHILIC_BOWL.get())), List.of(), Set.of(TipConcept.ETHER_STORAGE)));
        registerTip(StorageUpgrade.ID,
                new TipInfo(List.of(Ingredient.of(Items.CHEST), Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(ItemTags.COPPER_CHESTS))), List.of(), Set.of(TipConcept.LOGISTICS)));
        registerTip(EtherStreamStorageUpgrade.ID,
                new TipInfo(List.of(
                        Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(ItemTags.CHEST_BOATS)), Ingredient.of(Items.SPRUCE_CHEST_BOAT)
                ), List.of(), Set.of(TipConcept.ETHER_FLOW, TipConcept.LOGISTICS)));
        registerTip(EtherStreamStorageUpgrade.ID_1,
                new TipInfo(List.of(Ingredient.of(Items.CHEST_MINECART)), List.of(), Set.of(TipConcept.ETHER_FLOW, TipConcept.LOGISTICS)));
        registerTip(EtherStreamStorageUpgrade.ID_2,
                new TipInfo(List.of(
                        Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(ItemTags.SHULKER_BOXES))
                ), List.of(), Set.of(TipConcept.ETHER_FLOW, TipConcept.LOGISTICS)));
        registerTip(EtherStreamPreventDecayUpgrade.ID,
                new TipInfo(List.of(Ingredient.of(Items.REPEATER)), List.of(), Set.of(TipConcept.ETHER_FLOW)));
        registerTip(EtherStreamBreakBlockUpgrade.ID,
                new TipInfo(List.of(
                        Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(ItemTags.PICKAXES)),
                        Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(ItemTags.AXES)),
                        Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(ItemTags.SHOVELS)),
                        Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(ItemTags.HOES))
                ), List.of(), Set.of(TipConcept.WORLD_INTERACTION, TipConcept.ETHER_FLOW)));
        registerTip(EtherStreamDamageUpgrade.ID,
                new TipInfo(List.of(
                        Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(ItemTags.SWORDS))
                ), List.of(), Set.of(TipConcept.WORLD_INTERACTION, TipConcept.ETHER_FLOW)));
        registerTip(EtherFilterUpgrade.ID,
                new TipInfo(List.of(Ingredient.of(Items.PAPER)), List.of(), Set.of(TipConcept.LOGISTICS)));
        registerTip(EtherItemifyUpgrade.ID,
                new TipInfo(List.of(Ingredient.of(ItemRegistry.INACTIVATED_ETHER.get())), List.of(), Set.of(TipConcept.ETHER_STORAGE)));
        registerTip(EtherStreamTextUpgrade.ID,
                new TipInfo(List.of(Ingredient.of(Items.WRITTEN_BOOK)), List.of(), Set.of(TipConcept.DECORATION)));
        registerTip(EtherStreamGrowthAcceleratorUpgrade.ID,
                new TipInfo(List.of(Ingredient.of(Items.BONE_MEAL)), List.of(), Set.of(TipConcept.WORLD_INTERACTION, TipConcept.ETHER_FLOW)));
        registerTip(EtherStreamGrowthAcceleratorUpgrade.ID_ALL,
                new TipInfo(List.of(Ingredient.of(Items.SCULK_CATALYST)), List.of(), Set.of(TipConcept.WORLD_INTERACTION, TipConcept.ETHER_FLOW)));
        registerTip(EtherStreamCarryEntityUpgrade.ID,
                new TipInfo(List.of(
                        DifferenceIngredient.of(
                                Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(ItemTags.BOATS)),
                                Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(ItemTags.CHEST_BOATS))
                        )
                ), List.of(), Set.of(TipConcept.ETHER_FLOW, TipConcept.WORLD_INTERACTION)));
        registerTip(EtherStreamBounceBackUpgrade.ID,
                new TipInfo(List.of(Ingredient.of(Items.SLIME_BALL)), List.of(), Set.of(TipConcept.ETHER_FLOW)));

        scanNodeProcessRecipes(recipeManager);
    }

    private void scanNodeProcessRecipes(RecipeManager recipeManager) {
        List<ItemStack> allResults = new ArrayList<>();

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (holder.value() instanceof NodeProcessRecipe recipe) {
                allResults.add(recipe.result.create());
            }
        }

        registerTip(FunctionNodeProcess.ID, new TipInfo(
                List.of(Ingredient.of(Items.CRAFTER)),
                allResults,
                Set.of(TipConcept.CRAFTING)
        ));
    }

    public void registerTip(Identifier pluginId, TipInfo tipInfo) {
        tips.put(pluginId, tipInfo);
    }

    public Optional<TipInfo> getTip(Identifier pluginId) {
        return Optional.ofNullable(tips.get(pluginId));
    }

    public boolean hasTip(Identifier pluginId) {
        return tips.containsKey(pluginId);
    }

    public void setClientTips(Map<Identifier, TipInfo> data) {
        tips.clear();
        tips.putAll(data);
    }

    public Map<Identifier, TipInfo> getAllTips() {
        return Map.copyOf(tips);
    }
}
