package studio.fantasyit.ether_craft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.BlockRegistry;
import studio.fantasyit.ether_craft.register.ItemRegistry;
import studio.fantasyit.ether_craft.register.Tags;

import java.util.concurrent.CompletableFuture;

import static studio.fantasyit.ether_craft.register.Tags.PROCESS_CHIP;

public class TagGenBlock extends TagsProvider<Block> {

    protected TagGenBlock(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.BLOCK, lookupProvider, EtherCraft.MODID);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        getOrCreateRawBuilder(Tags.ETHER_STREAM_PASS_THROUGH)
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.GLASS));
        getOrCreateRawBuilder(Tags.ETHER_MACHINE)
                .addElement(BlockRegistry.ETHER_PROCESS_FACTORY.getKey().identifier())
                .addElement(BlockRegistry.ETHER_STREAM_EMITTER.getKey().identifier())
                .addElement(BlockRegistry.ETHER_ADAPT_NODE.getKey().identifier());

        getOrCreateRawBuilder(BlockTags.MINEABLE_WITH_PICKAXE)
                .addElement(BlockRegistry.ETHER_PROCESS_FACTORY.getKey().identifier())
                .addElement(BlockRegistry.ETHER_STREAM_EMITTER.getKey().identifier())
                .addElement(BlockRegistry.ETHER_ADAPT_NODE.getKey().identifier())
                .addElement(BlockRegistry.ETHER_GLASS.getKey().identifier())
                .addElement(BlockRegistry.ETHER_BLOCK.getKey().identifier())
                .addElement(BlockRegistry.ETHER_ORE.getKey().identifier())
                .addElement(BlockRegistry.DEEPSLATE_ETHER_ORE.getKey().identifier())
                .addElement(BlockRegistry.NETHER_ETHER_ORE.getKey().identifier());

        getOrCreateRawBuilder(BlockTags.NEEDS_IRON_TOOL)
                .addElement(BlockRegistry.ETHER_BLOCK.getKey().identifier())
                .addElement(BlockRegistry.ETHER_ORE.getKey().identifier())
                .addElement(BlockRegistry.DEEPSLATE_ETHER_ORE.getKey().identifier())
                .addElement(BlockRegistry.NETHER_ETHER_ORE.getKey().identifier());
    }
}
