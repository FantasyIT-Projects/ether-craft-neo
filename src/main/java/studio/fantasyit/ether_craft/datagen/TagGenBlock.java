package studio.fantasyit.ether_craft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.BlockRegistry;
import studio.fantasyit.ether_craft.register.Tags;

import java.util.concurrent.CompletableFuture;

public class TagGenBlock extends TagsProvider<Block> {

    protected TagGenBlock(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.BLOCK, lookupProvider, EtherCraft.MODID);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        getOrCreateRawBuilder(Tags.ETHER_STREAM_PASS_THROUGH)
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.GLASS))
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.GLASS_PANE))
                .addElement(BlockRegistry.ETHER_GLASS.getId());
        getOrCreateRawBuilder(Tags.ETHER_MACHINE)
                .addElement(BlockRegistry.ETHER_PROCESS_FACTORY.getKey().identifier())
                .addElement(BlockRegistry.ETHER_ADAPT_NODE.getKey().identifier());

        getOrCreateRawBuilder(Tags.ETHER_WRENCHABLE)
                .addElement(BlockRegistry.ETHER_PROCESS_FACTORY.getKey().identifier())
                .addElement(BlockRegistry.ETHER_ADAPT_NODE.getKey().identifier())
                .addElement(BlockRegistry.ETHER_GLASS.getKey().identifier())
                .addElement(BlockRegistry.ETHER_BLOCK.getKey().identifier())
                .addElement(BlockRegistry.INACTIVATED_ETHER_BLOCK.getKey().identifier())
                .addElement(BlockRegistry.SMOOTH_INACTIVATED_ETHER_BLOCK.getKey().identifier());

        getOrCreateRawBuilder(BlockTags.MINEABLE_WITH_PICKAXE)
                .addElement(BlockRegistry.ETHER_PROCESS_FACTORY.getKey().identifier())
                .addElement(BlockRegistry.ETHER_ADAPT_NODE.getKey().identifier())
                .addElement(BlockRegistry.ETHER_GLASS.getKey().identifier())
                .addElement(BlockRegistry.ETHER_BLOCK.getKey().identifier())
                .addElement(BlockRegistry.ETHER_ORE.getKey().identifier())
                .addElement(BlockRegistry.DEEPSLATE_ETHER_ORE.getKey().identifier())
                .addElement(BlockRegistry.NETHER_ETHER_ORE.getKey().identifier())
                .addElement(BlockRegistry.INACTIVATED_ETHER_BLOCK.getKey().identifier())
                .addElement(BlockRegistry.SMOOTH_INACTIVATED_ETHER_BLOCK.getKey().identifier());

        var cropBuilder = getOrCreateRawBuilder(Tags.CROP_ACCELERATABLE);
        BuiltInRegistries.BLOCK.forEach(block -> {
            if (block instanceof BonemealableBlock) {
                cropBuilder.addElement(BuiltInRegistries.BLOCK.getKey(block));
            }
        });
        cropBuilder.addElement(BuiltInRegistries.BLOCK.getKey(Blocks.CAVE_VINES));
        cropBuilder.addElement(BuiltInRegistries.BLOCK.getKey(Blocks.CAVE_VINES_PLANT));
        cropBuilder.addElement(BuiltInRegistries.BLOCK.getKey(Blocks.SUGAR_CANE));
        cropBuilder.addElement(BuiltInRegistries.BLOCK.getKey(Blocks.CACTUS));
        cropBuilder.addElement(BuiltInRegistries.BLOCK.getKey(Blocks.BAMBOO));
        cropBuilder.addElement(BuiltInRegistries.BLOCK.getKey(Blocks.BAMBOO_SAPLING));
        cropBuilder.addElement(BuiltInRegistries.BLOCK.getKey(Blocks.TWISTING_VINES));
        cropBuilder.addElement(BuiltInRegistries.BLOCK.getKey(Blocks.TWISTING_VINES_PLANT));
        cropBuilder.addElement(BuiltInRegistries.BLOCK.getKey(Blocks.WEEPING_VINES));
        cropBuilder.addElement(BuiltInRegistries.BLOCK.getKey(Blocks.WEEPING_VINES_PLANT));
        cropBuilder.addElement(BuiltInRegistries.BLOCK.getKey(Blocks.KELP));
        cropBuilder.addElement(BuiltInRegistries.BLOCK.getKey(Blocks.KELP_PLANT));
        cropBuilder.addElement(BuiltInRegistries.BLOCK.getKey(Blocks.CHORUS_FLOWER));

        getOrCreateRawBuilder(Tags.ETHER_STREAM_SKIP_BREAKING)
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.GLASS))
                .addElement(BlockRegistry.ETHER_GLASS.getId())
                .addElement(BlockRegistry.ETHER_PROCESS_FACTORY.getKey().identifier())
                .addElement(BlockRegistry.ETHER_ADAPT_NODE.getKey().identifier());

        getOrCreateRawBuilder(Tags.STONE_ABSORBABLE)
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.STONE))
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.COBBLESTONE))
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.DEEPSLATE))
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.COBBLED_DEEPSLATE))
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.GRANITE))
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.DIORITE))
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.ANDESITE))
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.TUFF))
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.CALCITE))
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.DRIPSTONE_BLOCK))
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.NETHERRACK))
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.BASALT))
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.SMOOTH_BASALT))
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.BLACKSTONE))
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.END_STONE))
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.SANDSTONE))
                .addElement(BuiltInRegistries.BLOCK.getKey(Blocks.RED_SANDSTONE));

        getOrCreateRawBuilder(BlockTags.NEEDS_IRON_TOOL)
                .addElement(BlockRegistry.ETHER_BLOCK.getKey().identifier())
                .addElement(BlockRegistry.ETHER_ORE.getKey().identifier())
                .addElement(BlockRegistry.DEEPSLATE_ETHER_ORE.getKey().identifier())
                .addElement(BlockRegistry.NETHER_ETHER_ORE.getKey().identifier())
                .addElement(BlockRegistry.INACTIVATED_ETHER_BLOCK.getKey().identifier())
                .addElement(BlockRegistry.SMOOTH_INACTIVATED_ETHER_BLOCK.getKey().identifier());
    }
}
