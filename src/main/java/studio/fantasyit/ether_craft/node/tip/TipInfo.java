package studio.fantasyit.ether_craft.node.tip;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record TipInfo(
        List<Ingredient> availableIngredients,
        List<ItemStack> producibleItems,
        Set<TipConcept> concepts
) {
    public static final Codec<TipInfo> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Ingredient.CODEC.listOf().fieldOf("available_ingredients").forGetter(TipInfo::availableIngredients),
            ItemStack.CODEC.listOf().fieldOf("producible_items").forGetter(TipInfo::producibleItems),
            TipConcept.CODEC.listOf()
                    .xmap(
                            list -> (Set<TipConcept>) new HashSet<>(list),
                            set -> new ArrayList<>(set)
                    )
                    .fieldOf("concepts")
                    .forGetter(TipInfo::concepts)
    ).apply(inst, TipInfo::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TipInfo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, Ingredient.CONTENTS_STREAM_CODEC),
            TipInfo::availableIngredients,
            ByteBufCodecs.collection(ArrayList::new, ItemStack.STREAM_CODEC),
            TipInfo::producibleItems,
            ByteBufCodecs.collection(HashSet::new, TipConcept.STREAM_CODEC),
            TipInfo::concepts,
            TipInfo::new
    );
}
