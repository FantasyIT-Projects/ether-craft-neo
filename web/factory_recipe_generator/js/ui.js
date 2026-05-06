// ui.js — All UI panel updates and palette interaction

const UI = {

    // ---- DOM refs (set in init) ----
    inputPanel: null,
    outputRowSel: null,
    outputItemInp: null,
    recipeFlow: null,
    statusEl: null,
    jsonOutput: null,
    paletteItems: null,
    customChipInp: null,

    init() {
        this.inputPanel = document.getElementById('input-slots');
        this.outputRowSel = document.getElementById('output-row');
        this.outputItemInp = document.getElementById('output-item');
        this.recipeFlow = document.getElementById('recipe-flow');
        this.statusEl = document.getElementById('status');
        this.jsonOutput = document.getElementById('json-output');
        this.paletteItems = document.querySelectorAll('.pal-item[data-chip]');
        this.customChipInp = document.getElementById('custom-chip');
    },

    // ---- Input Slots ----
    renderInputSlots() {
        if (!this.inputPanel) return;
        this.inputPanel.innerHTML = '';

        for (let i = 0; i < S.ROWS; i++) {
            const row = document.createElement('div');
            row.className = 'input-slot';
            if (i === S.outputRow) row.classList.add('active');

            const label = document.createElement('span');
            label.className = 'row-label';
            label.textContent = `R${i}`;

            const inp = document.createElement('input');
            inp.type = 'text';
            inp.value = S.inputItems[i] || '';
            inp.placeholder = 'modid:item';
            inp.addEventListener('input', (e) => {
                S.inputItems[i] = e.target.value;
                S.clearDetection();
                this.updateStatus();
            });

            // Highlight if this row is an input in the detected recipe
            if (S.detectedRecipe && S.detectedRecipe.inputIds.includes(i)) {
                row.classList.add('recipe-input');
            }

            row.appendChild(label);
            row.appendChild(inp);
            this.inputPanel.appendChild(row);
        }
    },

    // ---- Palette ----
    setSelected(chipId) {
        S.selectedChip = chipId;
        this.paletteItems.forEach(el => {
            const val = el.dataset.chip;
            el.classList.toggle('sel', val === chipId);
        });
        if (this.customChipInp) {
            this.customChipInp.value = (chipId && !S.CHIP_INFO[chipId] && chipId !== '' && chipId !== 'block') ? chipId : '';
        }
    },

    handlePaletteClick(chipVal) {
        if (chipVal === '__clr') {
            this.setSelected('');
        } else if (chipVal === '__blk') {
            this.setSelected('block');
        } else {
            this.setSelected(chipVal);
        }
    },

    handleCustomAdd() {
        const val = (this.customChipInp?.value || '').trim();
        if (val && val.includes(':')) {
            this.setSelected(val);
        }
    },

    // ---- Output ----
    get outputRow() {
        return parseInt(this.outputRowSel?.value || '4', 10);
    },

    get outputItem() {
        return (this.outputItemInp?.value || '').trim();
    },

    // ---- Recipe Flow Panel ----
    updateRecipePanel() {
        if (!this.recipeFlow) return;

        if (!S.detectedRecipe) {
            this.recipeFlow.innerHTML =
                '<span class="dim">Place chips on the grid, then click <b>Detect Recipe</b> to recognise the structure.</span>';
            return;
        }

        const r = Recipe.toJson(S.detectedRecipe);
        if (!r || r.process.length === 0) {
            this.recipeFlow.innerHTML =
                '<span class="dim">No process steps detected. Ensure empty cells form a connected path from output to the left edge, with chips adjacent.</span>';
            return;
        }

        let html = '';

        // Inputs
        for (const inp of r.input) {
            const label = typeof inp.item === 'string' ? inp.item : (inp.item.item || inp.item.tag || '?');
            html += `<div class="fnode input">
                <div class="fid">${h(inp.id)}</div>
                <div class="fitm">${h(label)}</div>
                <div class="fnxt">→ ${h(inp.next)}</div>
            </div>`;
        }

        // Process chain displayed in input→output direction (r.process is already ordered that way)
        for (let i = 0; i < r.process.length; i++) {
            const proc = r.process[i];
            const chipLabels = proc.item.map(c => {
                if (c.chip) return c.chip.split(':').pop();
                if (c.tag) return '#' + c.tag.split(':').pop();
                return JSON.stringify(c);
            }).join(', ');
            html += `<div class="farr">→</div>`;
            html += `<div class="fnode process">
                <div class="fid">${h(proc.id)}</div>
                <div class="fitm">${h(chipLabels)}</div>
                <div class="fnxt">→ ${h(proc.next)}</div>
            </div>`;
        }

        // Output
        html += `<div class="farr">→</div>`;
        html += `<div class="fnode output">
            <div class="fid">O</div>
            <div class="fitm">${h(r.output.item[0]?.id || '?')}</div>
        </div>`;

        this.recipeFlow.innerHTML = html;
    },

    // ---- Status Bar ----
    updateStatus() {
        if (!this.statusEl) return;

        if (!S.detectedRecipe) {
            this.statusEl.innerHTML = '';
            return;
        }

        const r = Recipe.toJson(S.detectedRecipe);
        if (!r) {
            this.statusEl.innerHTML =
                '<div class="bar warn">Could not build recipe from detected tree.</div>';
            return;
        }

        const inCnt = r.input.length;
        const procCnt = r.process.length;

        if (inCnt === 0) {
            this.statusEl.innerHTML =
                '<div class="bar warn">No inputs detected. Ensure the path reaches the left edge of the grid, and input items are filled in for those rows.</div>';
            return;
        }

        const hasCycle = !S.markMatrix || !Detection.markTreeArea(
            Detection.buildMarkMatrix(), S.COLS - 1, S.outputRow, -1, -1, S.outputRow + 1
        );

        if (hasCycle) {
            this.statusEl.innerHTML =
                '<div class="bar warn">Path has cycles or invalid regions. Use Block cells to constrain the path.</div>';
        } else {
            this.statusEl.innerHTML =
                `<div class="bar ok">Valid path: ${inCnt} input(s), ${procCnt} process step(s), 1 output.</div>`;
        }
    },

    // ---- JSON Output ----
    setJsonOutput(text) {
        if (this.jsonOutput) {
            this.jsonOutput.value = text;
        }
    },

    getJsonInput() {
        return (this.jsonOutput?.value || '').trim();
    },
};

// Short helper
function h(s) {
    if (s == null) return '';
    const div = document.createElement('div');
    div.textContent = String(s);
    return div.innerHTML;
}
