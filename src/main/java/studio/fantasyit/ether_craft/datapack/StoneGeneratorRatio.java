package studio.fantasyit.ether_craft.datapack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import studio.fantasyit.ether_craft.EtherCraft;

import javax.annotation.Nullable;

public record StoneGeneratorRatio(int burnTicks, int etherPerTick) {
    private static final Codec<StoneGeneratorRatio> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("burnTicks").forGetter(StoneGeneratorRatio::burnTicks),
                    Codec.INT.fieldOf("etherPerTick").forGetter(StoneGeneratorRatio::etherPerTick)
            ).apply(instance, StoneGeneratorRatio::new)
    );
    public static final DataMapType<Item, StoneGeneratorRatio> STONE_GENERATOR_RATIO = DataMapType.builder(
            EtherCraft.id("stone_generator_ratio"),
            Registries.ITEM,
            StoneGeneratorRatio.CODEC
    ).build();

    public static @Nullable StoneGeneratorRatio get(ItemStack item) {
        return item.typeHolder().getData(STONE_GENERATOR_RATIO);
    }
}
