// export.js — JSON export

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
};
