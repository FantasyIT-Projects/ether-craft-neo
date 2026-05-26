package studio.fantasyit.ether_craft.recipe.grid;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import studio.fantasyit.ether_craft.item.ProcessChipItem;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
import studio.fantasyit.ether_craft.register.Tags;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class EtherProcessFactoryGrid implements Recipe<EtherProcessFactoryGridInput> {
    public record Rect(int x, int y, int width, int height) {

    }

    ItemStackTemplate target;
    ItemStackTemplate[][] fullGrid;
    final int fullWidth;
    final int fullHeight;
    @Nullable
    Rect _rect;

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
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<? extends Recipe<EtherProcessFactoryGridInput>> getSerializer() {
        return null;
    }

    @Override
    public RecipeType<? extends Recipe<EtherProcessFactoryGridInput>> getType() {
        return null;
    }

    @Override
    public PlacementInfo placementInfo() {
        return null;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return null;
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
}
