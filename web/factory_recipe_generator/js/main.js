// main.js — Entry point, event binding, preload example

let autoDetectTimer = null;
let autoSaveTimer = null;

function runDetect() {
    const result = Detection.detectRecipes();
    S.detectedRecipe = result.recipes.length > 0 ? result.recipes[0] : null;
    UI.updateRecipePanel();   // calls toJson, sets processPositions on recipeData
    UI.renderInputSlots();    // re-render to show detected input highlights
    Grid.render();            // now processPositions is available for PID labels
    UI.updateStatus();
}

function scheduleAutoDetect() {
    if (!UI.autoDetectEl || !UI.autoDetectEl.checked) return;
    clearTimeout(autoDetectTimer);
    autoDetectTimer = setTimeout(runDetect, 300);
}

function scheduleAutoSave() {
    clearTimeout(autoSaveTimer);
    autoSaveTimer = setTimeout(() => {
        S.saveGrid();
    }, 2000);
}

function onGridChanged() {
    scheduleAutoDetect();
    scheduleAutoSave();
}

function onInputChanged() {
    scheduleAutoDetect();
    scheduleAutoSave();
}

function onOutputChanged() {
    scheduleAutoDetect();
    scheduleAutoSave();
}

document.addEventListener('DOMContentLoaded', () => {

    UI.init();
    Grid.init(document.getElementById('grid'));
    UI.renderRowBtns();
    UI.updateToolIndicator();
    UI.renderSavedChips();

    // --- restore auto-detect state ---
    if (UI.autoDetectEl) {
        const saved = localStorage.getItem('ether_factory_autodetect');
        UI.autoDetectEl.checked = saved === 'true';
    }

    // --- bind built-in palette items ---
    UI._refreshPaletteRefs();
    UI.paletteItems.forEach(el => {
        el.addEventListener('click', () => {
            UI.handlePaletteClick(el.dataset.chip);
            if (S.selectedChip === '') scheduleAutoDetect();
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
        UI.renderRowBtns();
        UI.renderInputSlots();
        Grid.render();
        UI.updateRecipePanel();
        UI.updateStatus();
        onOutputChanged();
    });

    UI.outputItemInp?.addEventListener('input', () => {
        S.outputItemId = UI.outputItem;
        onOutputChanged();
    });

    // --- button events ---
    document.getElementById('btn-detect')?.addEventListener('click', () => {
        runDetect();
        Export.exportJson();
    });

    document.getElementById('btn-export')?.addEventListener('click', () => {
        Export.exportJson();
    });

    document.getElementById('btn-save')?.addEventListener('click', () => {
        S.saveGrid();
        alert('Grid saved to browser storage.');
    });

    document.getElementById('btn-load')?.addEventListener('click', () => {
        if (S.hasSavedGrid()) {
            if (S.loadGrid()) {
                UI.outputRowSel.value = String(S.outputRow);
                UI.outputItemInp.value = S.outputItemId;
                UI.renderInputSlots();
                UI.renderRowBtns();
                Grid.render();
                runDetect();
                Export.exportJson();
            }
        } else {
            alert('No saved grid found in browser storage.');
        }
    });

    document.getElementById('btn-export-file')?.addEventListener('click', () => {
        const json = S.saveGrid();
        const blob = new Blob([json], {type: 'application/json'});
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = S.outputItemId.replace(/:/g, '_').replace(/['",{}]/g, "") + '.json';
        a.click();
        URL.revokeObjectURL(url);
    });

    document.getElementById('load-file')?.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = (ev) => {
            try {
                if (S.loadGrid(ev.target.result)) {
                    UI.outputRowSel.value = String(S.outputRow);
                    UI.outputItemInp.value = S.outputItemId;
                    UI.renderInputSlots();
                    UI.renderRowBtns();
                    Grid.render();
                    runDetect();
                    Export.exportJson();
                } else {
                    alert('Invalid grid file.');
                }
            } catch (err) {
                alert('Failed to load: ' + err.message);
            }
        };
        reader.readAsText(file);
        e.target.value = '';
    });

    document.getElementById('btn-clear-grid')?.addEventListener('click', () => {
        S.initGrid();
        S.clearDetection();
        Grid.render();
        UI.updateRecipePanel();
        UI.updateStatus();
        onGridChanged();
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
        onGridChanged();
    });

    // --- auto-detect toggle ---
    UI.autoDetectEl?.addEventListener('change', () => {
        const on = UI.autoDetectEl.checked;
        localStorage.setItem('ether_factory_autodetect', String(on));
        if (on) scheduleAutoDetect();
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
    S.initGrid();
    S.initInputs();
    S.outputRow = 4;
    S.outputItemId = 'minecraft:iron_ingot';
    if (UI.outputRowSel) UI.outputRowSel.value = '4';
    if (UI.outputItemInp) UI.outputItemInp.value = 'minecraft:iron_ingot';
    UI.setSelected('');
    UI.renderRowBtns();

    for (let x = 0; x < S.COLS; x++) {
        S.grid[2][x] = {type: 'block', chipId: null};
        S.grid[3][x] = {type: 'block', chipId: null};
        if (x !== 1 && x !== 3 && x !== 5) {
            S.grid[5][x] = {type: 'block', chipId: null};
        }
        S.grid[6][x] = {type: 'block', chipId: null};
    }

    S.grid[5][1] = {type: 'chip', chipId: 'ether_craft:heating_chip'};
    S.grid[5][3] = {type: 'chip', chipId: 'ether_craft:stamping_chip'};
    S.grid[5][5] = {type: 'chip', chipId: 'ether_craft:stamping_chip'};

    S.inputItems[4] = 'minecraft:raw_iron';

    UI.renderInputSlots();
    Grid.render();

    const result = Detection.detectRecipes();
    S.detectedRecipe = result.recipes.length > 0 ? result.recipes[0] : null;
    UI.updateRecipePanel();
    UI.renderInputSlots();
    Grid.render();
    UI.updateStatus();
    Export.exportJson();
}
