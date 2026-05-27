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
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
import studio.fantasyit.ether_craft.register.RecipeSerializerRegistry;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;
import studio.fantasyit.ether_craft.register.Tags;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class EtherProcessFactoryGrid implements Recipe<EtherProcessFactoryGridInput> {
    public record Rect(int x, int y, int width, int height) {

    }

    public static final MapCodec<EtherProcessFactoryGrid> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ItemStackTemplate.CODEC.fieldOf("target").forGetter(g -> g.target),
            Codec.list(Codec.list(ItemStackTemplate.CODEC, 1, Integer.MAX_VALUE), 1, Integer.MAX_VALUE)
                    .fieldOf("grid").forGetter(EtherProcessFactoryGrid::getGridAsList)
    ).apply(inst, EtherProcessFactoryGrid::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EtherProcessFactoryGrid> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC, g -> g.target,
            grid2DStreamCodec(), EtherProcessFactoryGrid::getGridAsList,
            EtherProcessFactoryGrid::new
    );

    private static StreamCodec<RegistryFriendlyByteBuf, List<List<ItemStackTemplate>>> grid2DStreamCodec() {
        StreamCodec<RegistryFriendlyByteBuf, List<ItemStackTemplate>> rowCodec =
                ByteBufCodecs.collection(ArrayList::new, ItemStackTemplate.STREAM_CODEC);
        return ByteBufCodecs.collection(ArrayList::new, rowCodec);
    }

    ItemStackTemplate target;
    ItemStackTemplate[][] fullGrid;
    final int fullWidth;
    final int fullHeight;
    @Nullable
    Rect _rect;

    public EtherProcessFactoryGrid(ItemStackTemplate target, List<List<ItemStackTemplate>> fullGrid) {
        this.target = target;
        this.fullHeight = fullGrid.size();
        this.fullWidth = fullGrid.isEmpty() ? 0 : fullGrid.get(0).size();
        this.fullGrid = new ItemStackTemplate[fullHeight][fullWidth];
        for (int i = 0; i < fullHeight; i++) {
            for (int j = 0; j < fullWidth; j++) {
                this.fullGrid[i][j] = fullGrid.get(i).get(j);
            }
        }
    }

    public EtherProcessFactoryGrid(ItemStackTemplate target, ItemStackTemplate[][] fullGrid) {
        this.target = target;
        this.fullGrid = fullGrid;
        if (fullGrid.length == 0)
            throw new IllegalArgumentException("fullGrid cannot be empty");
        this.fullWidth = fullGrid[0].length;
        this.fullHeight = fullGrid.length;
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
        for (int i = 0; i < etherProcessFactoryGridInput.w(); i++) {
            for (int j = 0; j < etherProcessFactoryGridInput.h(); j++) {
                if (i < rect.width && j < rect.height)
                    grid.get(j).set(i, fullGrid[j][i].create());
                else
                    grid.get(j).set(i, ProcessChipItem.getStackFor(ProcessChipItem.SEPARATOR));
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

    private Rect findRect() {
        int minX = fullWidth;
        int minY = fullHeight;
        int maxX = 0;
        int maxY = 0;
        for (int i = 0; i < fullWidth; i++) {
            for (int j = 0; j < fullHeight; j++) {
                ItemStack stack = fullGrid[j][i].create();
                if (stack.isEmpty() || !stack.is(Tags.PROCESS_CHIP))
                    continue;
                if (i < minX)
                    minX = i;
                if (i > maxX)
                    maxX = i;
                if (j < minY)
                    minY = j;
                if (j > maxY)
                    maxY = j;
            }
        }
        return new Rect(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    public List<List<ItemStackTemplate>> getGridAsList() {
        List<List<ItemStackTemplate>> result = new ArrayList<>(fullHeight);
        for (int i = 0; i < fullHeight; i++) {
            List<ItemStackTemplate> row = new ArrayList<>(fullWidth);
            for (int j = 0; j < fullWidth; j++) {
                row.add(fullGrid[i][j]);
            }
            result.add(row);
        }
        return result;
    }
}
