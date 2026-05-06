// state.js — Central mutable state for the Recipe Generator

const S = {
    ROWS: 9,
    COLS: 9,
    DIRS: [[0, 1], [1, 0], [0, -1], [-1, 0]],
    DIRECT_INPUT: 'ether_craft:direct_input',

    SEPARATOR_CHIP: 'ether_craft:separator_chip',

    CHIP_INFO: {
        'ether_craft:heating_chip':  { label: 'Heating',  cls: 'chip-heating',  color: '#e76f51' },
        'ether_craft:stamping_chip': { label: 'Stamping', cls: 'chip-stamping', color: '#f4a261' },
        'ether_craft:cutting_chip':  { label: 'Cutting',  cls: 'chip-cutting',  color: '#a0a0a0' },
        'ether_craft:fan_chip':      { label: 'Fan',      cls: 'chip-fan',      color: '#457b9d' },
        'ether_craft:separator_chip':{ label: 'Separator',cls: 'chip-separator',color: '#b39ddb' },
    },

    BUILTIN_CHIPS: [
        'ether_craft:heating_chip',
        'ether_craft:stamping_chip',
        'ether_craft:cutting_chip',
        'ether_craft:fan_chip',
        'ether_craft:separator_chip',
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

    isSeparator(chipId) {
        return chipId === this.SEPARATOR_CHIP;
    },
};
