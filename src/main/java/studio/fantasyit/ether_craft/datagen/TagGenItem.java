package studio.fantasyit.ether_craft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.concurrent.CompletableFuture;

import static studio.fantasyit.ether_craft.register.Tags.CONSUMABLE_EQUIPMENTS;
import static studio.fantasyit.ether_craft.register.Tags.PROCESS_CHIP;
import static studio.fantasyit.ether_craft.register.Tags.VINES;

public class TagGenItem extends TagsProvider<Item> {

    protected TagGenItem(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.ITEM, lookupProvider, EtherCraft.MODID);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        getOrCreateRawBuilder(PROCESS_CHIP)
                .addElement(ItemRegistry.PROCESS_CHIP_ITEM.getKey().identifier());

        var equip = getOrCreateRawBuilder(CONSUMABLE_EQUIPMENTS);
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.WOODEN_SWORD));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.STONE_SWORD));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.IRON_SWORD));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.GOLDEN_SWORD));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SWORD));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_SWORD));

        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.WOODEN_PICKAXE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.STONE_PICKAXE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.IRON_PICKAXE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.GOLDEN_PICKAXE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_PICKAXE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_PICKAXE));

        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.WOODEN_AXE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.STONE_AXE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.IRON_AXE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.GOLDEN_AXE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_AXE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_AXE));

        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.WOODEN_SHOVEL));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.STONE_SHOVEL));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.IRON_SHOVEL));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.GOLDEN_SHOVEL));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SHOVEL));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_SHOVEL));

        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.WOODEN_HOE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.STONE_HOE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.IRON_HOE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.GOLDEN_HOE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_HOE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_HOE));

        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.LEATHER_HELMET));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.CHAINMAIL_HELMET));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.IRON_HELMET));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.GOLDEN_HELMET));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_HELMET));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_HELMET));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.TURTLE_HELMET));

        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.LEATHER_CHESTPLATE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.CHAINMAIL_CHESTPLATE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.IRON_CHESTPLATE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.GOLDEN_CHESTPLATE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_CHESTPLATE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_CHESTPLATE));

        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.LEATHER_LEGGINGS));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.CHAINMAIL_LEGGINGS));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.IRON_LEGGINGS));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.GOLDEN_LEGGINGS));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_LEGGINGS));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_LEGGINGS));

        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.LEATHER_BOOTS));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.CHAINMAIL_BOOTS));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.IRON_BOOTS));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.GOLDEN_BOOTS));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_BOOTS));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_BOOTS));

        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.TRIDENT));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.BOW));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.CROSSBOW));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.FISHING_ROD));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.CARROT_ON_A_STICK));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.WARPED_FUNGUS_ON_A_STICK));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.SHEARS));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.FLINT_AND_STEEL));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.SHIELD));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.ELYTRA));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.MACE));
        equip.addElement(BuiltInRegistries.ITEM.getKey(Items.BRUSH));

        getOrCreateRawBuilder(VINES)
                .addElement(BuiltInRegistries.ITEM.getKey(Items.VINE))
                .addElement(BuiltInRegistries.ITEM.getKey(Items.TWISTING_VINES))
                .addElement(BuiltInRegistries.ITEM.getKey(Items.WEEPING_VINES));
    }
}
