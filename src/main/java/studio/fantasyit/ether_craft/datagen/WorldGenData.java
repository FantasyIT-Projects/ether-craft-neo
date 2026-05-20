package studio.fantasyit.ether_craft.datagen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.BlockRegistry;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class WorldGenData extends DatapackBuiltinEntriesProvider {
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_ETHER = ResourceKey.create(Registries.CONFIGURED_FEATURE, EtherCraft.id("ore_ether"));
    public static final ResourceKey<PlacedFeature> ORE_ETHER_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, EtherCraft.id("ore_ether"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_NETHER_ETHER = ResourceKey.create(Registries.CONFIGURED_FEATURE, EtherCraft.id("ore_nether_ether"));
    public static final ResourceKey<PlacedFeature> ORE_NETHER_ETHER_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, EtherCraft.id("ore_nether_ether"));
    public static final ResourceKey<BiomeModifier> OVERWORLD_ETHER_ORE_MODIFIER = ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, EtherCraft.id("ether_ore"));
    public static final ResourceKey<BiomeModifier> NETHER_ETHER_ORE_MODIFIER = ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, EtherCraft.id("nether_ether_ore"));

    public WorldGenData(PackOutput output, CompletableFuture<net.minecraft.core.HolderLookup.Provider> registries) {
        super(output, registries, new net.minecraft.core.RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, WorldGenData::bootstrapConfiguredFeatures)
                .add(Registries.PLACED_FEATURE, WorldGenData::bootstrapPlacedFeatures)
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, WorldGenData::bootstrapBiomeModifiers),
                Set.of(EtherCraft.MODID));
    }

    private static void bootstrapConfiguredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        var stoneOreReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        var deepslateOreReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        var netherrackReplaceables = new TagMatchTest(BlockTags.BASE_STONE_NETHER);

        var overworldTargets = List.of(
                OreConfiguration.target(stoneOreReplaceables, BlockRegistry.ETHER_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateOreReplaceables, BlockRegistry.DEEPSLATE_ETHER_ORE.get().defaultBlockState())
        );

        FeatureUtils.register(context, ORE_ETHER, Feature.ORE, new OreConfiguration(overworldTargets, 8));
        FeatureUtils.register(context, ORE_NETHER_ETHER, Feature.ORE, new OreConfiguration(netherrackReplaceables, BlockRegistry.NETHER_ETHER_ORE.get().defaultBlockState(), 8));
    }

    private static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        PlacementUtils.register(context, ORE_ETHER_PLACED,
                configuredFeatures.getOrThrow(ORE_ETHER),
                commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(32))));

        PlacementUtils.register(context, ORE_NETHER_ETHER_PLACED,
                configuredFeatures.getOrThrow(ORE_NETHER_ETHER),
                commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(128))));
    }

    private static void bootstrapBiomeModifiers(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);

        context.register(OVERWORLD_ETHER_ORE_MODIFIER, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placedFeatures.getOrThrow(ORE_ETHER_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES
        ));

        context.register(NETHER_ETHER_ORE_MODIFIER, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_NETHER),
                HolderSet.direct(placedFeatures.getOrThrow(ORE_NETHER_ETHER_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES
        ));
    }

    private static List<PlacementModifier> orePlacement(PlacementModifier countModifier, PlacementModifier heightRange) {
        return List.of(countModifier, InSquarePlacement.spread(), heightRange, BiomeFilter.biome());
    }

    private static List<PlacementModifier> commonOrePlacement(int count, PlacementModifier heightRange) {
        return orePlacement(CountPlacement.of(count), heightRange);
    }
}
