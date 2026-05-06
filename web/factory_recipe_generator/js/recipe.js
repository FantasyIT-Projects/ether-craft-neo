// recipe.js — Convert detected recipe tree to JSON matching EtherProcessRecipeJson format

const Recipe = {

    /**
     * Format an item into SizedIngredient-compatible JSON.
     * Accepts: "modid:item", "modid:item:2" (with count), "#tag", or full object.
     */
    formatItem(raw) {
        if (!raw || raw === '') return { item: 'minecraft:air' };
        if (typeof raw !== 'string') return raw;

        // Tag prefix
        if (raw.startsWith('#')) {
            return { tag: raw.slice(1) };
        }

        // "modid:item::count" pattern (double colon for count separation)
        const doubleMatch = raw.match(/^(.+?)::(\d+)$/);
        if (doubleMatch) {
            return { item: doubleMatch[1], count: parseInt(doubleMatch[2], 10) };
        }

        // "modid:item:count" pattern (single colon — ambiguous, prefer item only)
        // Only treat last segment as count if it's all digits and has 3+ segments
        const parts = raw.split(':');
        if (parts.length > 2 && /^\d+$/.test(parts[parts.length - 1])) {
            const count = parseInt(parts.pop(), 10);
            return { item: parts.join(':'), count };
        }

        return { item: raw };
    },

    /**
     * Convert a chip list from recipe tree to process item entries.
     * Each chip maps to { chip: "modid:chip_id" }.
     */
    formatChips(chips) {
        return chips.map(c => {
            if (c.chip) return { chip: c.chip };
            if (c.tag) return { tag: c.tag };
            return c;
        });
    },

    /**
     * Convert the detected RecipeData into the final JSON structure.
     *
     * The detected tree flows from output (root) toward inputs (leaves).
     * Recipe JSON flows from input toward output.
     * We reverse the process nodes for the recipe format.
     */
    toJson(recipeData) {
        if (!recipeData) return null;

        const processNodes = recipeData.processNodes;
        const inputIds = recipeData.inputIds;

        const inputJson = [];

        // processNodes is in output→input order (closest to output first).
        // Reverse so JSON lists process steps in input→output order (like blade.json).
        const ordered = [...processNodes].reverse();
        // ordered[0] is closest to input, ordered[last] is closest to output

        const processJson = [];
        for (let i = 0; i < ordered.length; i++) {
            const pid = `P${i}`;
            // Next step: if last step, connect to output; otherwise connect to previous (which is closer to output)
            const nextId = (i === ordered.length - 1) ? 'O' : `P${i + 1}`;
            processJson.push({
                id: pid,
                next: nextId,
                item: this.formatChips(ordered[i].chips),
            });
        }

        // Input entries connect to the first process step (closest to input)
        const firstProcId = ordered.length > 0 ? 'P0' : 'O';
        for (let i = 0; i < inputIds.length; i++) {
            const row = inputIds[i];
            const rawItem = (S.inputItems[row] || '').trim();
            inputJson.push({
                id: `I${i}`,
                item: this.formatItem(rawItem || 'minecraft:air'),
                next: firstProcId,
            });
        }

        return {
            type: 'ether_craft:ether_process',
            input: inputJson,
            process: processJson,
            output: {
                id: 'O',
                item: [{
                    id: (S.outputItemId || 'minecraft:air').trim(),
                }],
            },
        };
    },

    /**
     * Build a human-readable description of the recipe flow.
     */
    describe(recipeData) {
        if (!recipeData) return 'No recipe data.';

        const pn = recipeData.processNodes;
        const chipsByStep = pn.map(p => p.chips.map(c => (c.chip || c.tag || '?').split(':').pop()).join('+'));
        const inputs = recipeData.inputIds.map(id => S.inputItems[id] || '(empty)');

        return `Input[${inputs.join(', ')}] → ${chipsByStep.join(' → ')} → Output[${S.outputItemId}]`;
    },
};
