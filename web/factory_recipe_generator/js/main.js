// main.js — Entry point, event binding, preload example

document.addEventListener('DOMContentLoaded', () => {

    // --- init modules ---
    UI.init();
    Grid.init(document.getElementById('grid'));

    // --- palette events ---
    UI.paletteItems.forEach(el => {
        el.addEventListener('click', () => {
            UI.handlePaletteClick(el.dataset.chip);
        });
    });

    document.getElementById('btn-add-custom')?.addEventListener('click', () => {
        UI.handleCustomAdd();
    });

    UI.customChipInp?.addEventListener('input', () => {
        const val = UI.customChipInp.value.trim();
        if (val && val.includes(':')) {
            UI.setSelected(val);
        }
    });

    // --- output events ---
    UI.outputRowSel?.addEventListener('change', () => {
        S.outputRow = UI.outputRow;
        S.clearDetection();
        UI.renderInputSlots();
        Grid.render();
        UI.updateRecipePanel();
        UI.updateStatus();
    });

    UI.outputItemInp?.addEventListener('input', () => {
        S.outputItemId = UI.outputItem;
    });

    // --- button events ---
    document.getElementById('btn-detect')?.addEventListener('click', () => {
        const result = Detection.detectRecipes();
        S.detectedRecipe = result.recipes.length > 0 ? result.recipes[0] : null;
        Grid.render();
        UI.updateRecipePanel();
        UI.updateStatus();
    });

    document.getElementById('btn-export')?.addEventListener('click', () => {
        Export.exportJson();
    });

    document.getElementById('btn-import')?.addEventListener('click', () => {
        Export.importJson();
    });

    document.getElementById('btn-clear-grid')?.addEventListener('click', () => {
        S.initGrid();
        S.clearDetection();
        Grid.render();
        UI.updateRecipePanel();
        UI.updateStatus();
    });

    document.getElementById('btn-clear-all')?.addEventListener('click', () => {
        S.initGrid();
        S.initInputs();
        S.clearDetection();
        UI.renderInputSlots();
        Grid.render();
        UI.updateRecipePanel();
        UI.updateStatus();
        UI.setJsonOutput('');
    });

    // --- keyboard shortcuts ---
    document.addEventListener('keydown', (e) => {
        if (e.ctrlKey && e.key === 'd') {
            e.preventDefault();
            document.getElementById('btn-detect')?.click();
        }
        if (e.ctrlKey && e.key === 'e') {
            e.preventDefault();
            Export.exportJson();
        }
    });

    // --- preload example ---
    preloadExample();
});

function preloadExample() {
    // Example layout for the blade recipe:
    //   Row 3: all blocked (walls to constrain the path)
    //   Row 4: path cells (empty), output at col 8
    //   Row 5: chips placed below path cells, remainder blocked
    //   Row 6: all blocked
    //
    // Path: (8,4)→(7,4)→(6,4)→(5,4)→(4,4)→(3,4)→(2,4)→(1,4)→(0,4)→input row 4
    // Chips: (1,5)=heating, (3,5)=stamping, (5,5)=stamping

    S.initGrid();
    S.initInputs();
    S.outputRow = 4;
    S.outputItemId = 'minecraft:iron_ingot';
    if (UI.outputRowSel) UI.outputRowSel.value = '4';
    if (UI.outputItemInp) UI.outputItemInp.value = S.outputItemId;
    UI.setSelected('');

    // Walls above and below to constrain the path
    for (let x = 0; x < S.COLS; x++) {
        S.grid[2][x] = { type: 'block', chipId: null };
        S.grid[3][x] = { type: 'block', chipId: null };
        if (x !== 1 && x !== 3 && x !== 5) {
            S.grid[5][x] = { type: 'block', chipId: null };
        }
        S.grid[6][x] = { type: 'block', chipId: null };
    }

    // Row 5 chips (adjacent to path cells in row 4)
    S.grid[5][1] = { type: 'chip', chipId: 'ether_craft:heating_chip' };
    S.grid[5][3] = { type: 'chip', chipId: 'ether_craft:stamping_chip' };
    S.grid[5][5] = { type: 'chip', chipId: 'ether_craft:stamping_chip' };

    // Input item for row 4
    S.inputItems[4] = 'minecraft:raw_iron';

    UI.renderInputSlots();
    Grid.render();

    // Auto-detect
    const result = Detection.detectRecipes();
    if (result.recipes.length > 0) {
        S.detectedRecipe = result.recipes[0];
    }
    Grid.render();
    UI.updateRecipePanel();
    UI.updateStatus();
    Export.exportJson();
}
