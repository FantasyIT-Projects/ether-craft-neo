package studio.fantasyit.ether_craft.menu.grid;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryAsset;

import java.util.List;

public class ViewGridScreen extends Screen {
    ItemStack[][] grid;
    final int gridWidth;
    final int gridHeight;
    final int totalWidth;
    final int totalHeight;

    protected ViewGridScreen(Component title, List<List<ItemStack>> grid) {
        super(title);
        gridHeight = grid.size();
        gridWidth = grid.isEmpty() ? 0 : grid.get(0).size();
        totalWidth = gridWidth * 18;
        totalHeight = gridHeight * 18;
        this.grid = new ItemStack[gridHeight][gridWidth];
        for (int i = 0; i < gridHeight; i++) {
            for (int j = 0; j < gridWidth; j++) {
                this.grid[i][j] = grid.get(i).get(j);
            }
        }
    }


    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int left = (width - totalWidth) / 2;
        int top = (height - totalHeight) / 2;
        for (int i = 0; i < gridHeight; i++) {
            for (int j = 0; j < gridWidth; j++) {
                EtherProcessFactoryAsset.SLOT.blit(graphics, left + j * 18, top + i * 18);
                graphics.item(grid[i][j], left + j * 18 + 1, top + i * 18 + 1);
            }
        }

    }

    @Override
    public boolean isInGameUi() {
        return true;
    }
}
