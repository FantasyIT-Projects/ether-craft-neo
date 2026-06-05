package studio.fantasyit.ether_craft.recipe.plating;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

import static studio.fantasyit.ether_craft.recipe.IngredientSerializer.VANILLA_COMPAT_SIZED_INGREDIENT_CODEC;

public record PlatingRecipeJson(List<SizedIngredient> input, Identifier effect, Ingredient filter) {
    public static final MapCodec<PlatingRecipeJson> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            VANILLA_COMPAT_SIZED_INGREDIENT_CODEC.listOf().fieldOf("input").forGetter(PlatingRecipeJson::input),
            Identifier.CODEC.fieldOf("effect").forGetter(PlatingRecipeJson::effect),
            Ingredient.CODEC.fieldOf("filter").forGetter(PlatingRecipeJson::filter)
    ).apply(instance, PlatingRecipeJson::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlatingRecipeJson> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), PlatingRecipeJson::input,
            Identifier.STREAM_CODEC, PlatingRecipeJson::effect,
            Ingredient.CONTENTS_STREAM_CODEC, PlatingRecipeJson::filter,
            PlatingRecipeJson::new
    );
}