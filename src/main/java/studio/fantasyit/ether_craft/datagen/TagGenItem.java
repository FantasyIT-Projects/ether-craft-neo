package studio.fantasyit.ether_craft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.concurrent.CompletableFuture;

import static studio.fantasyit.ether_craft.register.Tags.PROCESS_CHIP;

public class TagGenItem extends TagsProvider<Item> {

    protected TagGenItem(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.ITEM, lookupProvider, EtherCraft.MODID);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        getOrCreateRawBuilder(PROCESS_CHIP)
                .addElement(ItemRegistry.PROCESS_CHIP_ITEM.getKey().identifier());
    }
}
