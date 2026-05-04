package studio.fantasyit.ether_craft.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import studio.fantasyit.ether_craft.item.ProcessChipItem;

public class IngredientSerializer {
    public interface SizedIngredientLike {
        SizedIngredient toIngredient();
    }

    public record ChipRecord(Identifier id) implements SizedIngredientLike {
        @Override
        public SizedIngredient toIngredient() {
            return new SizedIngredient(
                    DataComponentIngredient.of(
                            true, ProcessChipItem.getStackFor(id())
                    ),
                    1
            );
        }
    }

    public static final Codec<ChipRecord> CHIP_RECORD_CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    Identifier.CODEC.fieldOf("chip").forGetter(ChipRecord::id)
            ).apply(inst, ChipRecord::new));

    public static final Codec<DelayedIngredient> CHIP_INGREDIENT_CODEC = Codec.xor(
            CHIP_RECORD_CODEC.xmap(
                    i -> (SizedIngredientLike) i,
                    i -> (ChipRecord) i
            ),
            SizedIngredient.NESTED_CODEC.withAlternative(
                    Ingredient.CODEC.xmap(ingredient -> new SizedIngredient(ingredient, 1), SizedIngredient::ingredient)
            )
    ).xmap(DelayedIngredient::new, DelayedIngredient::ingredient);

    public static final Codec<SizedIngredient> VANILLA_COMPAT_SIZED_INGREDIENT_CODEC = Codec.either(
            Ingredient.CODEC,
            SizedIngredient.NESTED_CODEC
    ).xmap(
            either -> either.map(
                    ingredient -> new SizedIngredient(ingredient, 1),
                    sizedIngredient -> sizedIngredient
            ),
            Either::right
    );
}
