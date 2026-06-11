package studio.fantasyit.ether_craft.datagen;

import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jspecify.annotations.NonNull;
import studio.fantasyit.ether_craft.register.BlockRegistry;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
import studio.fantasyit.ether_craft.register.ItemRegistry;

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
            add(BlockRegistry.ETHER_ORE.get(), createEtherOreDrop(BlockRegistry.ETHER_ORE.get(), ItemRegistry.INACTIVATED_ETHER.get()));
            add(BlockRegistry.DEEPSLATE_ETHER_ORE.get(), createEtherOreDrop(BlockRegistry.DEEPSLATE_ETHER_ORE.get(), ItemRegistry.INACTIVATED_ETHER.get()));
            add(BlockRegistry.NETHER_ETHER_ORE.get(), createEtherOreDrop(BlockRegistry.NETHER_ETHER_ORE.get(), ItemRegistry.INACTIVATED_ETHER.get()));
            dropSelf(BlockRegistry.ETHER_GLASS.get());
        }

        private LootItemCondition.Builder hasPlating() {
            return MatchTool.toolMatches(ItemPredicate.Builder.item()
                    .withComponents(DataComponentMatchers.Builder.components()
                            .any(DataComponentRegistry.PLATING_DATA.get())
                            .build()));
        }

        private LootTable.Builder createEtherOreDrop(Block original, Item drop) {
            return createOreDrop(original, drop)
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0F))
                            .when(hasPlating())
                            .add(LootItem.lootTableItem(ItemRegistry.ETHER_CRYSTAL.get())
                                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))));
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
