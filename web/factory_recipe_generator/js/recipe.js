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
        if (!raw || raw === '') return 'minecraft:air';
        if (typeof raw !== 'string') return raw;
        const parsed = this.tryParseJson(raw);
        if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) return parsed;
        // Count syntax: modid:item::N or #tag::N => SizedIngredient.NESTED_CODEC format
        const dm = raw.match(/^(.+?)::(\d+)$/);
        if (dm) {
            const id = dm[1];
            const count = parseInt(dm[2], 10);
            return { ingredient: id, count: count };
        }
        return raw;
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

        // Build tree node → {x,y} position map from process nodes
        const nodePos = new Map();
        for (const pn of recipeData.processNodes || []) {
            if (pn.treeNodeId != null && pn.x != null) {
                nodePos.set(pn.treeNodeId, { x: pn.x, y: pn.y });
            }
        }

        const nodePid = new Map();
        const processList = [];
        const inputList = [];
        const processPositions = [];   // [{pid, x, y}]
        let pc = 0;

        const self = this;

        function walk(nodeId) {
            const edges = tree.getEdges(nodeId);
            for (const edge of edges) {
                const childId = edge.node.id;
                const isDirect = edge.value.length === 1 && edge.value[0] && edge.value[0].chip === S.DIRECT_INPUT;

                if (isDirect) {
                    if (nodeId === 0) {
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
                    const pos = nodePos.get(childId);
                    if (pos) processPositions.push({ pid, x: pos.x, y: pos.y });
                }

                walk(childId);
            }
        }

        walk(0);

        // Store positions on recipeData so Grid.render can overlay labels
        recipeData.processPositions = processPositions;

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
