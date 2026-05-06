// detection.js — Recipe detection algorithm
// Ported from EtherProcessorRecipeUtil.processFactoryInput / markTreeArea / scanForTrees

const Detection = {

    /**
     * Build the mark matrix from the current grid.
     *   0   = empty, traversable
     *  -1   = chip (blocks traversal, adjacent chips provide processing)
     *  100  = block / wall (blocks traversal)
     */
    buildMarkMatrix() {
        const mark = Array.from({ length: S.ROWS }, () => Array(S.COLS).fill(0));
        for (let y = 0; y < S.ROWS; y++) {
            for (let x = 0; x < S.COLS; x++) {
                const cell = S.grid[y][x];
                if (cell.type === 'chip' && cell.chipId) {
                    mark[y][x] = -1;
                } else if (cell.type === 'block') {
                    mark[y][x] = 100;
                } else {
                    mark[y][x] = 0;
                }
            }
        }
        return mark;
    },

    /**
     * Flood-fill through connected empty cells (value 0), marking them with markId.
     * Cells with -1 (chip) or 100 (block) are skipped.
     * Returns false if a cycle or invalid region is detected.
     */
    markTreeArea(mark, x, y, fromX, fromY, markId) {
        if (y < 0 || y >= S.ROWS || x < 0 || x >= S.COLS) return false;
        if (mark[y][x] !== 0) return false;

        let valid = true;
        mark[y][x] = markId;

        for (let i = 0; i < 4; i++) {
            const x2 = x + S.DIRS[i][0];
            const y2 = y + S.DIRS[i][1];
            if (x2 === fromX && y2 === fromY) continue;
            if (x2 >= 0 && x2 < S.COLS && y2 >= 0 && y2 < S.ROWS) {
                const v = mark[y2][x2];
                if (v === 0) {
                    if (!this.markTreeArea(mark, x2, y2, x, y, markId)) valid = false;
                } else if (v !== -1 && v !== 100) {
                    // Already marked by a different path → cycle
                    valid = false;
                }
                // v === -1 or v === 100 → just skip
            }
        }
        return valid;
    },

    /**
     * After markTreeArea, walk through the marked area and build the recipe tree.
     * At each path cell (marked with markId), gather chips from the 4 adjacent cells.
     * When reaching x == -1 (left edge), register an input item.
     *
     * @returns {RecipeData} with inputIds, processNodes, pathCells, chipCells, tree
     */
    scanForTrees(mark, tree, x, y, fromX, fromY, markId, parentId) {
        if (x === -1) {
            this._recipeData.inputIds.push(y);
            const id = tree.maxId + 1;
            tree.addNode(id, []);
            tree.addEdge(parentId, id, [{ chip: S.DIRECT_INPUT }]);
            this._recipeData.inputTreeIds.push(id);
            return;
        }

        this._recipeData.pathCells.add(`${x},${y}`);

        // Gather chips from adjacent cells (except the direction we came from)
        const chips = [];
        for (let i = 0; i < 4; i++) {
            const x2 = x + S.DIRS[i][0];
            const y2 = y + S.DIRS[i][1];
            if (x2 === fromX && y2 === fromY) continue;
            if (x2 >= 0 && x2 < S.COLS && y2 >= 0 && y2 < S.ROWS) {
                const cell = S.grid[y2][x2];
                if (cell.type === 'chip' && cell.chipId) {
                    if (!S.isSeparator(cell.chipId)) {
                        chips.push({ chip: cell.chipId });
                    }
                    this._recipeData.chipCells.add(`${x2},${y2}`);
                }
            }
        }

        let curParent = parentId;
        if (chips.length > 0) {
            const pn = {
                id: S.newId(),
                chips,
                parentId,
            };
            this._recipeData.processNodes.push(pn);

            const nextId = tree.maxId + 1;
            tree.addNode(nextId, []);
            tree.addEdge(parentId, nextId, chips);
            curParent = nextId;
        }

        // Recurse to neighbouring path cells
        for (let i = 0; i < 4; i++) {
            const x2 = x + S.DIRS[i][0];
            const y2 = y + S.DIRS[i][1];
            if (x2 === fromX && y2 === fromY) continue;
            if (x2 >= -1 && x2 < S.COLS && y2 >= 0 && y2 < S.ROWS) {
                if (x2 === -1 || mark[y2][x2] === markId) {
                    this.scanForTrees(mark, tree, x2, y2, x, y, markId, curParent);
                }
            }
        }
    },

    /** Running state during scanForTrees */
    _recipeData: null,

    /**
     * Main detection function.
     * @returns {{ recipes: RecipeData[], markMatrix: number[][], leakingSpeed: number }}
     */
    detectRecipes() {
        S.resetIds();

        const result = {
            recipes: [],
            markMatrix: this.buildMarkMatrix(),
            leakingSpeed: 0,
        };

        const mark = result.markMatrix;
        const outRow = S.outputRow;
        const markId = outRow + 1;

        let illegal = false;

        if (!this.markTreeArea(mark, S.COLS - 1, outRow, -1, -1, markId)) {
            illegal = true;
        }

        // Ensure no other output row shares this mark
        if (!illegal) {
            for (let j = 0; j < S.ROWS; j++) {
                if (j !== outRow && mark[j][S.COLS - 1] === markId) {
                    illegal = true;
                    break;
                }
            }
        }

        if (illegal) {
            let inCnt = 0, outCnt = 0;
            for (let j = 0; j < S.ROWS; j++) {
                if (mark[j][0] === markId) inCnt++;
                if (mark[j][S.COLS - 1] === markId) outCnt++;
            }
            result.leakingSpeed = inCnt * outCnt * 10;
        } else {
            const tree = new RecipeTree(0, []);
            tree.addNode(1, []);
            tree.addEdge(0, 1, [{ chip: S.DIRECT_INPUT }]);

            this._recipeData = {
                inputIds: [],
                inputTreeIds: [],
                processNodes: [],
                pathCells: new Set(),
                chipCells: new Set(),
                tree,
                outputRow: outRow,
            };

            this.scanForTrees(mark, tree, S.COLS - 1, outRow, -1, -1, markId, 1);

            const rd = this._recipeData;
            if (rd.processNodes.length > 0) {
                result.recipes.push(rd);
            }
            this._recipeData = null;
        }

        S.markMatrix = mark;
        return result;
    },
};
