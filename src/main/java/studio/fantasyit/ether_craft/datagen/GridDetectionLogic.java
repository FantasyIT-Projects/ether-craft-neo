package studio.fantasyit.ether_craft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.recipe.DelayedIngredient;
import studio.fantasyit.ether_craft.recipe.IngredientSerializer.ChipRecord;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeJson;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeJson.InputEntry;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeJson.OutputEntry;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeJson.ProcessEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of detection.js + tree.js + recipe.js.
 * Uses flood-fill to detect recipe paths through the grid,
 * then builds an ether_process recipe JSON.
 */
public class GridDetectionLogic {

    private static final int[][] DIRS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

    private final List<Integer> inputRowIds = new ArrayList<>();
    private final List<ProcessEntryBuilder> processEntries = new ArrayList<>();
    private int pidCounter = 0;

    private record ProcessEntryBuilder(String id, List<DelayedIngredient> chips, String next) {}

    static EtherProcessRecipeJson process(HolderLookup.Provider registries, GridSourceFile source) {
        var logic = new GridDetectionLogic();
        return logic.detect(registries, source);
    }

    private EtherProcessRecipeJson detect(HolderLookup.Provider registries, GridSourceFile source) {
        List<List<GridSourceFile.Cell>> grid = source.grid();
        int rows = grid.size();
        int cols = grid.get(0).size();
        int outputRow = source.outputRow();

        int[][] mark = buildMarkMatrix(grid, rows, cols);

        int markId = outputRow + 1;

        if (!markTreeArea(mark, cols - 1, outputRow, -1, -1, markId, rows, cols)) {
            return null;
        }

        for (int j = 0; j < rows; j++) {
            if (j != outputRow && mark[j][cols - 1] == markId) {
                return null;
            }
        }

        scanForTrees(mark, source, cols - 1, outputRow, -1, -1, markId, "O");

        if (processEntries.isEmpty() && inputRowIds.isEmpty()) {
            return null;
        }

        return buildRecipeJson(registries, source);
    }

    private int[][] buildMarkMatrix(List<List<GridSourceFile.Cell>> grid, int rows, int cols) {
        int[][] mark = new int[rows][cols];
        for (int y = 0; y < rows; y++) {
            List<GridSourceFile.Cell> row = grid.get(y);
            for (int x = 0; x < cols; x++) {
                GridSourceFile.Cell cell = row.get(x);
                if (cell.isChip()) mark[y][x] = -1;
                else if (cell.isBlock()) mark[y][x] = 100;
                else mark[y][x] = 0;
            }
        }
        return mark;
    }

    private boolean markTreeArea(int[][] mark, int x, int y, int fromX, int fromY, int markId, int rows, int cols) {
        if (y < 0 || y >= rows || x < 0 || x >= cols) return false;
        if (mark[y][x] != 0) return false;

        boolean valid = true;
        mark[y][x] = markId;

        for (int[] dir : DIRS) {
            int x2 = x + dir[0];
            int y2 = y + dir[1];
            if (x2 == fromX && y2 == fromY) continue;
            if (x2 >= 0 && x2 < cols && y2 >= 0 && y2 < rows) {
                int v = mark[y2][x2];
                if (v == 0) {
                    if (!markTreeArea(mark, x2, y2, x, y, markId, rows, cols)) valid = false;
                } else if (v != -1 && v != 100 && v != markId) {
                    valid = false;
                }
            }
        }
        return valid;
    }

    private void scanForTrees(int[][] mark, GridSourceFile source, int x, int y, int fromX, int fromY, int markId, String parentPid) {
        int cols = mark[0].length;

        if (x == -1) {
            inputRowIds.add(y);
            return;
        }

        List<GridSourceFile.Cell> gridRow = source.grid().get(y);

        List<DelayedIngredient> chips = new ArrayList<>();
        for (int[] dir : DIRS) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (nx == fromX && ny == fromY) continue;
            if (nx >= 0 && nx < cols && ny >= 0 && ny < mark.length) {
                GridSourceFile.Cell cell = source.grid().get(ny).get(nx);
                if (cell.isChip()) {
                    chips.add(DelayedIngredient.of(new ChipRecord(Identifier.parse(cell.chipId()))));
                }
            }
        }

        String curPid = parentPid;
        if (!chips.isEmpty()) {
            String pid = "P" + pidCounter++;
            processEntries.add(new ProcessEntryBuilder(pid, chips, parentPid));
            curPid = pid;
        }

        for (int[] dir : DIRS) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (nx == fromX && ny == fromY) continue;
            if (nx >= -1 && nx < cols && ny >= 0 && ny < mark.length) {
                if (nx == -1 || mark[ny][nx] == markId) {
                    scanForTrees(mark, source, nx, ny, x, y, markId, curPid);
                }
            }
        }
    }

    private EtherProcessRecipeJson buildRecipeJson(HolderLookup.Provider registries, GridSourceFile source) {
        List<InputEntry> inputEntries = new ArrayList<>();
        for (int i = 0; i < inputRowIds.size(); i++) {
            int rowY = inputRowIds.get(i);
            String raw = rowY < source.inputItems().size() ? source.inputItems().get(rowY).trim() : "";
            SizedIngredient item = ItemFormatParser.parseInputForDetection(registries, raw);
            String next = processEntries.isEmpty() ? "O" : processEntries.get(processEntries.size() - 1).id;
            inputEntries.add(new InputEntry("I" + i, item, next));
        }

        List<ProcessEntry> reversedProcess = new ArrayList<>();
        for (int i = processEntries.size() - 1; i >= 0; i--) {
            ProcessEntryBuilder pe = processEntries.get(i);
            reversedProcess.add(new ProcessEntry(pe.id, pe.chips, pe.next));
        }

        List<ItemStackTemplate> outputs = ItemFormatParser.parseOutputItems(source.outputItemId());
        OutputEntry outputEntry = new OutputEntry("O", outputs);

        return new EtherProcessRecipeJson(inputEntries, outputEntry, reversedProcess);
    }
}
