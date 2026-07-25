package studio.fantasyit.ether_craft.datagen;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Recipe;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.recipe.grid.EtherProcessFactoryGrid;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class GridRecipeProvider extends RecipeProvider {

    private static final Path SOURCE_DIR = Path.of("../grid_source");

    protected GridRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        Path resolvedDir = SOURCE_DIR;
        if (!Files.isDirectory(resolvedDir)) {
            resolvedDir = Path.of("grid_source");
        }
        if (!Files.isDirectory(resolvedDir)) {
            EtherCraft.LOGGER.warn("GridRecipeProvider: grid_source directory not found");
            return;
        }

        try (Stream<Path> files = Files.list(resolvedDir)) {
            files.filter(f -> f.getFileName().toString().endsWith(".json"))
                    .forEach(this::processFile);
        } catch (IOException e) {
            EtherCraft.LOGGER.error("GridRecipeProvider: failed to read grid_source", e);
        }
    }

    private void processFile(Path file) {
        String fileName = file.getFileName().toString();
        String name = fileName.substring(0, fileName.length() - ".json".length());

        GridSourceFile source;
        try {
            String raw = Files.readString(file);
            JsonElement json = GsonHelper.parse(raw);
            source = GridSourceFile.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        } catch (Exception e) {
            EtherCraft.LOGGER.error("GridRecipeProvider: failed to parse {}: {}", fileName, e.getMessage());
            return;
        }

        EtherProcessFactoryGrid gridRecipe = GridExporterLogic.toGridRecipe(registries, source);
        output.accept(
                ResourceKey.create(Registries.RECIPE, EtherCraft.id("grid/" + name)),
                gridRecipe,
                null
        );

        EtherProcessRecipeJson processJson = GridDetectionLogic.process(registries, source);
        if (processJson != null) {
            EtherProcessFactoryRecipe processRecipe = new EtherProcessFactoryRecipe(processJson);
            output.accept(
                    ResourceKey.create(Registries.RECIPE, EtherCraft.id("ether_process/" + name)),
                    processRecipe,
                    null
            );
        }
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new GridRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Grid Recipes";
        }
    }
}
