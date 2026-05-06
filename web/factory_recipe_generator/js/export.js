// export.js — JSON export and import logic

const Export = {

    exportJson() {
        if (!S.detectedRecipe) {
            UI.setJsonOutput('// No recipe detected. Place chips, then click "Detect Recipe".');
            return;
        }
        const r = Recipe.toJson(S.detectedRecipe);
        if (!r) {
            UI.setJsonOutput('// Could not build recipe from tree.');
            return;
        }
        UI.setJsonOutput(JSON.stringify(r, null, 2));
    },

    importJson() {
        const text = UI.getJsonInput();
        if (!text) return;

        let data;
        try {
            data = JSON.parse(text);
        } catch (e) {
            alert('Invalid JSON: ' + e.message);
            return;
        }

        if (!data || data.type !== 'ether_craft:ether_process') {
            alert('Invalid recipe: "type" must be "ether_craft:ether_process".');
            return;
        }

        // Clear everything
        S.initGrid();
        S.initInputs();
        S.clearDetection();

        // Parse output
        if (data.output && data.output.item && data.output.item.length > 0) {
            S.outputItemId = data.output.item[0].id || '';
            if (UI.outputItemInp) UI.outputItemInp.value = S.outputItemId;
        }

        // Parse process entries — place chips accordingly
        // Since grid positions are not stored in JSON, we make a best-effort visual layout:
        // - Path through output row
        // - Chips one row below, spaced 2 columns apart
        if (data.process && data.process.length > 0) {
            const totalSteps = data.process.length;
            const startCol = Math.max(0, S.COLS - 1 - totalSteps * 2);
            const chipRow = (S.outputRow + 1) % S.ROWS;

            for (let pi = 0; pi < totalSteps; pi++) {
                const proc = data.process[pi];
                const col = startCol + pi * 2;
                if (col >= S.COLS) break;

                for (const item of proc.item) {
                    const chipId = item.chip || null;
                    if (chipId && col < S.COLS) {
                        S.grid[chipRow][col] = { type: 'chip', chipId };
                    }
                }
            }
        }

        // Parse inputs
        if (data.input) {
            data.input.forEach((inp, idx) => {
                const row = idx % S.ROWS;
                const itemStr = typeof inp.item === 'string'
                    ? inp.item
                    : (inp.item.item || inp.item.tag || '');
                S.inputItems[row] = itemStr;
            });
        }

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
    },
};
