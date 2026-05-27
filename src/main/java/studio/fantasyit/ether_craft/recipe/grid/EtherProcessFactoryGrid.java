package studio.fantasyit.ether_craft.recipe.grid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.item.ProcessChipItem;
import studio.fantasyit.ether_craft.recipe.DelayedIngredient;
import studio.fantasyit.ether_craft.recipe.IngredientSerializer;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
import studio.fantasyit.ether_craft.register.RecipeSerializerRegistry;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;
import studio.fantasyit.ether_craft.register.Tags;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class EtherProcessFactoryGrid implements Recipe<EtherProcessFactoryGridInput> {
    public ItemStack getTarget() {
        return target.create();
    }

    public record Rect(int x, int y, int width, int height) {
    }

    public record GridEntry(int x, int y, ItemStackTemplate item) {
        public static final Codec<GridEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.fieldOf("x").forGetter(GridEntry::x),
                Codec.INT.fieldOf("y").forGetter(GridEntry::y),
                ItemStackTemplate.CODEC.fieldOf("item").forGetter(GridEntry::item)
        ).apply(inst, GridEntry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, GridEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, GridEntry::x,
                ByteBufCodecs.VAR_INT, GridEntry::y,
                ItemStackTemplate.STREAM_CODEC, GridEntry::item,
                GridEntry::new
        );
    }

    public static final MapCodec<EtherProcessFactoryGrid> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ItemStackTemplate.CODEC.fieldOf("target").forGetter(g -> g.target),
            Codec.list(GridEntry.CODEC).fieldOf("entries").forGetter(EtherProcessFactoryGrid::getEntries),
            Codec.list(IngredientSerializer.CHIP_INGREDIENT_CODEC).optionalFieldOf("inputs", List.of()).forGetter(g -> g.inputs)
    ).apply(inst, EtherProcessFactoryGrid::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EtherProcessFactoryGrid> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC, g -> g.target,
            entriesStreamCodec(), EtherProcessFactoryGrid::getEntries,
            inputsStreamCodec(), EtherProcessFactoryGrid::getInputs,
            EtherProcessFactoryGrid::new
    );

    private static StreamCodec<RegistryFriendlyByteBuf, List<GridEntry>> entriesStreamCodec() {
        return ByteBufCodecs.collection(ArrayList::new, GridEntry.STREAM_CODEC);
    }

    private static StreamCodec<RegistryFriendlyByteBuf, DelayedIngredient> delayedIngredientStreamCodec() {
        return SizedIngredient.STREAM_CODEC.map(
                DelayedIngredient::of,
                DelayedIngredient::toIngredient
        );
    }

    private static StreamCodec<RegistryFriendlyByteBuf, List<DelayedIngredient>> inputsStreamCodec() {
        return ByteBufCodecs.collection(ArrayList::new, delayedIngredientStreamCodec());
    }

    ItemStackTemplate target;
    List<GridEntry> entries;
    List<DelayedIngredient> inputs;
    @Nullable
    Rect _rect;

    public EtherProcessFactoryGrid(ItemStackTemplate target, List<GridEntry> entries, List<DelayedIngredient> inputs) {
        this.target = target;
        this.entries = entries;
        this.inputs = inputs;
    }

    public List<GridEntry> getEntries() {
        return entries;
    }

    public List<DelayedIngredient> getInputs() {
        return inputs;
    }

    @Override
    public boolean matches(EtherProcessFactoryGridInput etherProcessFactoryGridInput, Level level) {
        Rect rect = getRect();
        if (rect.height > etherProcessFactoryGridInput.h() || rect.width > etherProcessFactoryGridInput.w())
            return false;
        return true;
    }

    @Override
    public ItemStack assemble(EtherProcessFactoryGridInput etherProcessFactoryGridInput) {
        ItemStack itemStack = etherProcessFactoryGridInput.target().copyWithCount(1);
        List<List<ItemStack>> grid = new ArrayList<>();
        for (int i = 0; i < etherProcessFactoryGridInput.h(); i++) {
            grid.add(new ArrayList<>());
            for (int j = 0; j < etherProcessFactoryGridInput.w(); j++) {
                grid.get(i).add(ItemStack.EMPTY);
            }
        }
        Rect rect = getRect();
        for (GridEntry entry : entries) {
            int x = entry.x - rect.x;
            int y = entry.y - rect.y;
            if (x >= 0 && x < etherProcessFactoryGridInput.w() && y >= 0 && y < etherProcessFactoryGridInput.h()) {
                grid.get(y).set(x, entry.item.create());
            }
        }
        itemStack.set(DataComponentRegistry.GRID, grid);
        return itemStack;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public @NotNull String group() {
        return "";
    }

    @Override
    public @NotNull RecipeSerializer<? extends Recipe<EtherProcessFactoryGridInput>> getSerializer() {
        return RecipeSerializerRegistry.ETHER_PROCESS_FACTORY_GRID_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<EtherProcessFactoryGridInput>> getType() {
        return RecipeTypeRegistry.ETHER_PROCESS_FACTORY_GRID.get();
    }

    @Override
    public @NotNull PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public @NotNull RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    public Rect getRect() {
        if (_rect == null)
            _rect = findRect();
        return _rect;
    }

    private static int[][] DIRECTIONS = {
            {0, 1},
            {0, -1},
            {1, 0},
            {-1, 0}
    };

    private Rect findRect() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean found = false;
        boolean[][] isChip = new boolean[16][16];
        boolean[][] hasItem = new boolean[16][16];
        for (GridEntry entry : entries) {
            ItemStack stack = entry.item.create();
            if (stack.isEmpty() || !stack.is(Tags.PROCESS_CHIP))
                continue;
            hasItem[entry.y][entry.x] = true;
            if (stack.has(DataComponentRegistry.CHIP_ID) && stack.get(DataComponentRegistry.CHIP_ID).equals(ProcessChipItem.SEPARATOR))
                continue;
            found = true;
            isChip[entry.y][entry.x] = true;
        }
        if (!found) return new Rect(0, 0, 0, 0);
        boolean[][] affected = new boolean[16][16];

        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                if (isChip[i][j]) {
                    for (int[] direction : DIRECTIONS) {
                        int x = i + direction[0];
                        int y = j + direction[1];
                        int xx = x + direction[0];
                        int yy = y + direction[1];
                        if (xx >= 0 && xx < 16 && yy >= 0 && yy < 16 && hasItem[xx][yy] && !hasItem[x][y]) {
                            affected[x][y] = true;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                if (!hasItem[i][j])
                    continue;
                if (!isChip[i][j]) {
                    boolean anyAffected = false;
                    for (int[] direction : DIRECTIONS) {
                        int x = i + direction[0];
                        int y = j + direction[1];
                        if (x >= 0 && x < 16 && y >= 0 && y < 16 && affected[x][y]) {
                            anyAffected = true;
                            break;
                        }
                    }
                    if (!anyAffected)
                        continue;
                }
                if (j < minX) minX = j;
                if (j > maxX) maxX = j;
                if (i < minY) minY = i;
                if (i > maxY) maxY = i;
            }
        }
        return new Rect(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
