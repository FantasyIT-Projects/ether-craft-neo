package studio.fantasyit.ether_craft.recipe.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.register.RecipeSerializerRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpgradeShapedRecipe extends NormalCraftingRecipe {
    private static final Codec<Character> CHAR_CODEC = Codec.STRING.xmap(
            s -> s.charAt(0),
            String::valueOf
    );

    public static final MapCodec<UpgradeShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
            CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
            ShapedRecipePattern.Data.MAP_CODEC.forGetter(o -> o.patternData),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result),
            CHAR_CODEC.optionalFieldOf("copyComponentsFrom", ' ').forGetter(o -> o.copyComponentsFrom)
    ).apply(inst, UpgradeShapedRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, ShapedRecipePattern.Data> PATTERN_DATA_STREAM_CODEC =
            StreamCodec.of(
                    (buf, data) -> {
                        Map<Character, Ingredient> key = data.key();
                        buf.writeVarInt(key.size());
                        for (var entry : key.entrySet()) {
                            buf.writeChar(entry.getKey());
                            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, entry.getValue());
                        }
                        List<String> pattern = data.pattern();
                        buf.writeVarInt(pattern.size());
                        for (String line : pattern) {
                            buf.writeUtf(line);
                        }
                    },
                    buf -> {
                        int keySize = buf.readVarInt();
                        Map<Character, Ingredient> key = new HashMap<>();
                        for (int i = 0; i < keySize; i++) {
                            key.put(buf.readChar(), Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                        }
                        int patternSize = buf.readVarInt();
                        List<String> pattern = new ArrayList<>();
                        for (int i = 0; i < patternSize; i++) {
                            pattern.add(buf.readUtf());
                        }
                        return new ShapedRecipePattern.Data(key, pattern);
                    }
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeShapedRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    CommonInfo.STREAM_CODEC, o -> o.commonInfo,
                    CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
                    PATTERN_DATA_STREAM_CODEC, o -> o.patternData,
                    ItemStackTemplate.STREAM_CODEC, o -> o.result,
                    StreamCodec.of(
                            (buf, ch) -> buf.writeChar(ch),
                            buf -> buf.readChar()
                    ), o -> o.copyComponentsFrom,
                    UpgradeShapedRecipe::new
            );

    private final ShapedRecipePattern.Data patternData;
    private final ShapedRecipePattern pattern;
    private final ItemStackTemplate result;
    private final char copyComponentsFrom;

    public UpgradeShapedRecipe(CommonInfo commonInfo, CraftingBookInfo bookInfo,
                               ShapedRecipePattern.Data patternData, ItemStackTemplate result,
                               char copyComponentsFrom) {
        super(commonInfo, bookInfo);
        this.patternData = patternData;
        this.pattern = ShapedRecipePattern.of(patternData.key(), patternData.pattern());
        this.result = result;
        this.copyComponentsFrom = copyComponentsFrom;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return this.pattern.matches(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack resultStack = this.result.create();
        if (copyComponentsFrom != ' ') {
            ItemStack source = findSourceItem(input);
            if (source != null && !source.isEmpty()) {
                resultStack.applyComponents(source.getComponentsPatch());
            }
        }
        return resultStack;
    }

    private ItemStack findSourceItem(CraftingInput input) {
        List<String> rawPattern = patternData.pattern();
        int patternHeight = rawPattern.size();
        int patternWidth = rawPattern.get(0).length();
        int gridHeight = input.height();
        int gridWidth = input.width();

        for (int offsetY = 0; offsetY <= gridHeight - patternHeight; offsetY++) {
            for (int offsetX = 0; offsetX <= gridWidth - patternWidth; offsetX++) {
                if (matchesAt(input, offsetX, offsetY)) {
                    for (int py = 0; py < patternHeight; py++) {
                        String line = rawPattern.get(py);
                        for (int px = 0; px < patternWidth; px++) {
                            if (line.charAt(px) == copyComponentsFrom) {
                                return input.getItem(offsetX + px, offsetY + py);
                            }
                        }
                    }
                    return null;
                }
            }
        }
        return null;
    }

    private boolean matchesAt(CraftingInput input, int offsetX, int offsetY) {
        List<String> rawPattern = patternData.pattern();
        int patternHeight = rawPattern.size();
        int patternWidth = rawPattern.get(0).length();

        for (int py = 0; py < patternHeight; py++) {
            String line = rawPattern.get(py);
            for (int px = 0; px < patternWidth; px++) {
                char symbol = line.charAt(px);
                ItemStack item = input.getItem(offsetX + px, offsetY + py);
                if (symbol == ' ') {
                    if (!item.isEmpty())
                        return false;
                } else {
                    Ingredient ingredient = patternData.key().get(symbol);
                    if (ingredient == null || !ingredient.test(item))
                        return false;
                }
            }
        }
        return true;
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.createFromOptionals(this.pattern.ingredients());
    }

    @Override
    public @NotNull RecipeSerializer<UpgradeShapedRecipe> getSerializer() {
        return RecipeSerializerRegistry.UPGRADE_SHAPED_RECIPE_SERIALIZER.get();
    }
}
