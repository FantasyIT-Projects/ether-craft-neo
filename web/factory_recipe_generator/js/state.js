// state.js — Central mutable state for the Recipe Generator

const S = {
    ROWS: 9,
    COLS: 9,
    DIRS: [[0, 1], [1, 0], [0, -1], [-1, 0]],
    DIRECT_INPUT: 'ether_craft:direct_input',

    CHIP_INFO: {
        'ether_craft:heating_chip':  { label: 'Heating',  cls: 'chip-heating',  color: '#e76f51' },
        'ether_craft:stamping_chip': { label: 'Stamping', cls: 'chip-stamping', color: '#f4a261' },
        'ether_craft:cutting_chip':  { label: 'Cutting',  cls: 'chip-cutting',  color: '#a0a0a0' },
        'ether_craft:fan_chip':      { label: 'Fan',      cls: 'chip-fan',      color: '#457b9d' },
    },

    BUILTIN_CHIPS: [
        'ether_craft:heating_chip',
        'ether_craft:stamping_chip',
        'ether_craft:cutting_chip',
        'ether_craft:fan_chip',
    ],

    // Each cell: { type: 'empty' | 'chip' | 'block', chipId: string | null }
    grid: [],
    inputItems: [],         // string[9], each row's input item id
    outputRow: 4,           // 0-8
    outputItemId: 'minecraft:iron_ingot',
    selectedChip: '',       // '' = clear/erase, 'block' = place block, chipId = place chip

    detectedRecipe: null,   // result of detection or null
    markMatrix: null,       // int[9][9] from last detection

    nextNodeId: 0,
    newId() { return this.nextNodeId++; },
    resetIds() { this.nextNodeId = 0; },

    initGrid() {
        this.grid = Array.from({ length: this.ROWS }, () =>
            Array.from({ length: this.COLS }, () => ({ type: 'empty', chipId: null }))
        );
    },

    initInputs() {
        this.inputItems = Array(this.ROWS).fill('');
    },

    clearDetection() {
        this.detectedRecipe = null;
        this.markMatrix = null;
    },

    chipInfo(chipId) {
        return this.CHIP_INFO[chipId] || null;
    },

    // ---- Custom Chips (localStorage) ----
    SAVED_KEY: 'ether_factory_custom_chips',
    GRID_KEY: 'ether_factory_grid',

    loadSavedChips() {
        try {
            const raw = localStorage.getItem(this.SAVED_KEY);
            return raw ? JSON.parse(raw) : [];
        } catch (_) { return []; }
    },

    saveCustomChip(chipId) {
        const chips = this.loadSavedChips();
        if (!chips.includes(chipId)) {
            chips.push(chipId);
            localStorage.setItem(this.SAVED_KEY, JSON.stringify(chips));
        }
    },

    removeCustomChip(chipId) {
        let chips = this.loadSavedChips();
        chips = chips.filter(c => c !== chipId);
        localStorage.setItem(this.SAVED_KEY, JSON.stringify(chips));
    },

    // ---- Grid Save/Load ----
    saveGrid() {
        const data = {
            grid: this.grid,
            inputItems: this.inputItems,
            outputRow: this.outputRow,
            outputItemId: this.outputItemId,
        };
        const json = JSON.stringify(data);
        localStorage.setItem(this.GRID_KEY, json);
        return json;
    },

    loadGrid(json) {
        let data;
        if (typeof json === 'string') {
            data = JSON.parse(json);
        } else {
            const raw = localStorage.getItem(this.GRID_KEY);
            if (!raw) return false;
            data = JSON.parse(raw);
        }
        if (!data || !data.grid) return false;
        this.grid = data.grid;
        this.inputItems = data.inputItems || Array(this.ROWS).fill('');
        this.outputRow = data.outputRow != null ? data.outputRow : 4;
        this.outputItemId = data.outputItemId || 'minecraft:iron_ingot';
        this.clearDetection();
        for (let y = 0; y < this.ROWS; y++) {
            for (let x = 0; x < this.COLS; x++) {
                const cell = this.grid[y][x];
                if (cell.type === 'chip' && cell.chipId) {
                    if (!this.BUILTIN_CHIPS.includes(cell.chipId)) {
                        const saved = this.loadSavedChips();
                        if (!saved.includes(cell.chipId)) {
                            this.saveCustomChip(cell.chipId);
                        }
                    }
                }
            }
        }
        return true;
    },

    hasSavedGrid() {
        return !!localStorage.getItem(this.GRID_KEY);
    },
};
