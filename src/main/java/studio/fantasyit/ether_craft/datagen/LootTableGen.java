package studio.fantasyit.ether_craft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.jspecify.annotations.NonNull;
import studio.fantasyit.ether_craft.register.BlockRegistry;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class LootTableGen extends LootTableProvider {
    public LootTableGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(), List.of(
                new SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK)
        ), registries);
    }

    public static class ModBlockLoot extends BlockLootSubProvider {
        protected ModBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }

        @Override
        protected void generate() {
            dropSelf(BlockRegistry.ETHER_BLOCK.get());
            add(BlockRegistry.ETHER_ORE.get(), createOreDrop(BlockRegistry.ETHER_ORE.get(), BlockRegistry.ETHER_ORE.get().asItem()));
            add(BlockRegistry.DEEPSLATE_ETHER_ORE.get(), createOreDrop(BlockRegistry.DEEPSLATE_ETHER_ORE.get(), BlockRegistry.DEEPSLATE_ETHER_ORE.get().asItem()));
            add(BlockRegistry.NETHER_ETHER_ORE.get(), createOreDrop(BlockRegistry.NETHER_ETHER_ORE.get(), BlockRegistry.NETHER_ETHER_ORE.get().asItem()));
            dropSelf(BlockRegistry.ETHER_GLASS.get());
        }

        @Override
        protected @NonNull Iterable<Block> getKnownBlocks() {
            return List.of(
                    BlockRegistry.ETHER_BLOCK.get(),
                    BlockRegistry.ETHER_ORE.get(),
                    BlockRegistry.DEEPSLATE_ETHER_ORE.get(),
                    BlockRegistry.NETHER_ETHER_ORE.get(),
                    BlockRegistry.ETHER_GLASS.get()
            );
        }
    }
}
