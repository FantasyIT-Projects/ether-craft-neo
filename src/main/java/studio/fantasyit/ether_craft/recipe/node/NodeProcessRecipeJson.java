package studio.fantasyit.ether_craft.recipe.node;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.ArrayList;
import java.util.List;

import static studio.fantasyit.ether_craft.recipe.IngredientSerializer.VANILLA_COMPAT_SIZED_INGREDIENT_CODEC;

public record NodeProcessRecipeJson(
        List<SizedIngredient> ingredients,
        ItemStackTemplate result,
        int etherCost
) {
    public static final MapCodec<NodeProcessRecipeJson> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.list(VANILLA_COMPAT_SIZED_INGREDIENT_CODEC).fieldOf("ingredients").forGetter(NodeProcessRecipeJson::ingredients),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(NodeProcessRecipeJson::result),
            Codec.INT.optionalFieldOf("etherCost", 0).forGetter(NodeProcessRecipeJson::etherCost)
    ).apply(inst, NodeProcessRecipeJson::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, NodeProcessRecipeJson> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, SizedIngredient.STREAM_CODEC), NodeProcessRecipeJson::ingredients,
            ItemStackTemplate.STREAM_CODEC, NodeProcessRecipeJson::result,
            ByteBufCodecs.INT, NodeProcessRecipeJson::etherCost,
            NodeProcessRecipeJson::new
    );
}
