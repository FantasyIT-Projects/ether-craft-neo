package studio.fantasyit.ether_craft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStackTemplate;
import studio.fantasyit.ether_craft.recipe.DelayedIngredient;
import studio.fantasyit.ether_craft.recipe.grid.EtherProcessFactoryGrid;
import studio.fantasyit.ether_craft.recipe.grid.EtherProcessFactoryGrid.GridEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of exporter.js — converts grid cells to ether_process_factory_grid recipes.
 */
public class GridExporterLogic {

    /**
     * Convert a GridSourceFile into an EtherProcessFactoryGrid recipe.
     * Maps chip cells → process_chip entries with chipId components.
     * Maps block cells → separator_chip entries.
     * Maps inputItems → DelayedIngredient list.
     */
    static EtherProcessFactoryGrid toGridRecipe(HolderLookup.Provider registries, GridSourceFile source) {
        ItemStackTemplate target = ItemFormatParser.parseOutputTarget(
                source.outputItemId() != null ? source.outputItemId() : "minecraft:air"
        );

        List<GridEntry> entries = new ArrayList<>();
        List<List<GridSourceFile.Cell>> grid = source.grid();
        for (int y = 0; y < grid.size(); y++) {
            List<GridSourceFile.Cell> row = grid.get(y);
            for (int x = 0; x < row.size(); x++) {
                GridSourceFile.Cell cell = row.get(x);
                if (cell.isChip()) {
                    entries.add(new GridEntry(x, y, ItemFormatParser.makeChipTemplate(cell.chipId())));
                } else if (cell.isBlock()) {
                    entries.add(new GridEntry(x, y, ItemFormatParser.makeSeparatorTemplate()));
                }
            }
        }

        List<DelayedIngredient> inputs = new ArrayList<>();
        for (String raw : source.inputItems()) {
            if (raw == null || raw.isEmpty()) continue;
            var si = ItemFormatParser.parseInputSizedIngredient(registries, raw);
            if (si != null) {
                inputs.add(DelayedIngredient.of(si));
            }
        }

        return new EtherProcessFactoryGrid(target, entries, inputs);
    }
}
