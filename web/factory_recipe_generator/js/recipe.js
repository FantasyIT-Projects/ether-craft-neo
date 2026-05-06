// recipe.js — Convert detected recipe tree to JSON matching EtherProcessRecipeJson format

const Recipe = {

    tryParseJson(raw) {
        if (!raw || typeof raw !== 'string') return raw;
        const trimmed = raw.trim();
        if ((trimmed.startsWith('{') || trimmed.startsWith('[')) && (trimmed.endsWith('}') || trimmed.endsWith(']'))) {
            try { return JSON.parse(trimmed); } catch (_) { }
        }
        return null;
    },

    formatItem(raw) {
        if (!raw || raw === '') return { item: 'minecraft:air' };
        if (typeof raw !== 'string') return raw;
        const parsed = this.tryParseJson(raw);
        if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) return parsed;
        if (raw.startsWith('#')) return { tag: raw.slice(1) };
        const dm = raw.match(/^(.+?)::(\d+)$/);
        if (dm) return { item: dm[1], count: parseInt(dm[2], 10) };
        return { item: raw };
    },

    parseOutput(raw) {
        if (!raw || !raw.trim()) return [{ id: 'minecraft:air' }];
        const trimmed = raw.trim();
        if ((trimmed.startsWith('{') || trimmed.startsWith('[')) &&
            (trimmed.endsWith('}') || trimmed.endsWith(']'))) {
            try {
                const p = JSON.parse(trimmed);
                if (Array.isArray(p)) return p.map(o => typeof o === 'string' ? { id: o } : o);
                if (typeof p === 'object') return [p];
            } catch (_) { }
        }
        if (trimmed.includes('\n')) {
            return trimmed.split('\n').map(s => s.trim()).filter(s => s)
                .map(s => { const p = this.tryParseJson(s); return (p && typeof p === 'object' && !Array.isArray(p)) ? p : { id: s }; });
        }
        return [{ id: trimmed }];
    },

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
     * The recipe is a tree rooted at the output:
     *   Root(0) --[direct_input]--> Node(1) --[chips]--> Node(N) ...
     * Each edge with chips becomes a process entry.
     * Each leaf connected via direct_input becomes an input entry.
     * The `next` field follows the tree parent→child (recipe: input→output) direction.
     */
    toJson(recipeData) {
        if (!recipeData) return null;

        const tree = recipeData.tree;
        const inputIds = recipeData.inputIds;
        const inputTreeIds = recipeData.inputTreeIds || [];

        const nodePid = new Map();
        const processList = [];
        const inputList = [];
        let pc = 0;

        const self = this;   // capture Recipe reference for nested walk()

        function walk(nodeId) {
            const edges = tree.getEdges(nodeId);
            for (const edge of edges) {
                const childId = edge.node.id;
                const isDirect = edge.value.length === 1 && edge.value[0] && edge.value[0].chip === S.DIRECT_INPUT;

                if (isDirect) {
                    if (nodeId === 0) {
                        // Root → Node1: skip
                    } else {
                        const idx = inputTreeIds.indexOf(childId);
                        if (idx >= 0) {
                            const row = inputIds[idx];
                            const raw = (S.inputItems[row] || '').trim();
                            inputList.push({
                                id: `I${idx}`,
                                item: self.formatItem(raw || 'minecraft:air'),
                                next: nodePid.get(nodeId) || 'O',
                            });
                        }
                    }
                } else if (edge.value.length > 0) {
                    const pid = `P${pc++}`;
                    nodePid.set(childId, pid);
                    processList.push({
                        id: pid,
                        item: self.formatChips(edge.value),
                        next: nodePid.get(nodeId) || 'O',
                    });
                }

                walk(childId);
            }
        }

        walk(0);

        // Reverse process entries so they read input→output (cosmetic, matches blade.json style)
        processList.reverse();

        return {
            type: 'ether_craft:ether_process',
            input: inputList,
            process: processList,
            output: {
                id: 'O',
                item: this.parseOutput(S.outputItemId || 'minecraft:air'),
            },
        };
    },

    describe(recipeData) {
        if (!recipeData) return 'No recipe data.';
        const pn = recipeData.processNodes;
        const chipsByStep = pn.map(p => p.chips.map(c => (c.chip || c.tag || '?').split(':').pop()).join('+'));
        const inputs = recipeData.inputIds.map(id => S.inputItems[id] || '(empty)');
        return `Input[${inputs.join(', ')}] → ${chipsByStep.join(' → ')} → Output[${S.outputItemId}]`;
    },
};
