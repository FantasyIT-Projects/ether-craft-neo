package studio.fantasyit.ether_craft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.DataMapProvider;
import studio.fantasyit.ether_craft.datapack.StoneGeneratorRatio;

import java.util.concurrent.CompletableFuture;

public class DataMapGen extends DataMapProvider {
    public DataMapGen(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        this.builder(StoneGeneratorRatio.STONE_GENERATOR_RATIO)
                .replace(true)
                .add(Items.COBBLESTONE.builtInRegistryHolder(), new StoneGeneratorRatio(100, 25), false)
                .add(Items.BASALT.builtInRegistryHolder(), new StoneGeneratorRatio(200, 25), false);
    }
}
