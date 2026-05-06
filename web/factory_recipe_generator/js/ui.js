// ui.js — All UI panel updates and palette interaction

const UI = {

    // ---- DOM refs (set in init) ----
    inputPanel: null,
    rowBtns: null,
    outputRowSel: null,
    outputItemInp: null,
    recipeFlow: null,
    statusEl: null,
    jsonOutput: null,
    paletteItems: null,
    customChipInp: null,
    toolInd: null,

    init() {
        this.inputPanel = document.getElementById('input-slots');
        this.rowBtns = document.getElementById('row-btns');
        this.outputRowSel = document.getElementById('output-row');
        this.outputItemInp = document.getElementById('output-item');
        this.recipeFlow = document.getElementById('recipe-flow');
        this.statusEl = document.getElementById('status');
        this.jsonOutput = document.getElementById('json-output');
        this.customChipInp = document.getElementById('custom-chip');
        this.toolInd = document.getElementById('tool-indicator');
        this.savedChipsEl = document.getElementById('saved-chips');
        this.autoDetectEl = document.getElementById('auto-detect');
        if (this.outputItemInp) {
            this.outputItemInp.value = 'minecraft:iron_ingot';
        }
        this._refreshPaletteRefs();
    },

    _refreshPaletteRefs() {
        this.paletteItems = document.querySelectorAll('.pal-item[data-chip], .saved-chip[data-chip]');
    },

    // ---- Input Slots ----
    renderInputSlots() {
        if (!this.inputPanel) return;
        this.inputPanel.innerHTML = '';

        for (let i = 0; i < S.ROWS; i++) {
            const row = document.createElement('div');
            row.className = 'input-slot';
            if (S.detectedRecipe && S.detectedRecipe.inputIds.includes(i)) {
                row.classList.add('detected');
            }

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
                if (typeof scheduleAutoDetect === 'function') scheduleAutoDetect();
                if (typeof scheduleAutoSave === 'function') scheduleAutoSave();
            });

            row.appendChild(label);
            row.appendChild(inp);
            this.inputPanel.appendChild(row);
        }
    },

    // ---- Output Row Buttons ----
    renderRowBtns() {
        if (!this.rowBtns) return;
        this.rowBtns.innerHTML = '';

        for (let i = 0; i < S.ROWS; i++) {
            const btn = document.createElement('button');
            btn.className = 'row-btn';
            if (i === S.outputRow) btn.classList.add('active');
            btn.textContent = `R${i}`;
            btn.title = `Set output row to ${i}`;
            btn.addEventListener('click', () => {
                S.outputRow = i;
                if (this.outputRowSel) this.outputRowSel.value = String(i);
                S.clearDetection();
                this.renderRowBtns();
                this.renderInputSlots();
                Grid.render();
                this.updateRecipePanel();
                this.updateStatus();
            });
            this.rowBtns.appendChild(btn);
        }
    },

    // ---- Palette ----
    setSelected(chipId) {
        S.selectedChip = chipId;
        this._refreshPaletteRefs();
        this.paletteItems.forEach(el => {
            const val = el.dataset.chip;
            el.classList.toggle('sel', val === chipId);
        });
        if (this.customChipInp) {
            this.customChipInp.value = (chipId && !S.CHIP_INFO[chipId] && chipId !== '' && chipId !== 'block') ? chipId : '';
        }
        this.updateToolIndicator();
    },

    updateToolIndicator() {
        if (!this.toolInd) return;
        const sel = S.selectedChip;
        if (!sel || sel === '') {
            this.toolInd.textContent = 'Current: Clear (Erase)';
            this.toolInd.className = 'tool-indicator';
        } else if (sel === 'block') {
            this.toolInd.textContent = 'Current: Block (Wall)';
            this.toolInd.className = 'tool-indicator has-tool';
        } else {
            const info = S.chipInfo(sel);
            const name = info ? info.label : sel.split(':').pop();
            this.toolInd.textContent = `Current: ${name} Chip`;
            this.toolInd.className = 'tool-indicator has-tool';
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
            S.saveCustomChip(val);
            this.renderSavedChips();
            this.setSelected(val);
        }
    },

    renderSavedChips() {
        if (!this.savedChipsEl) return;
        const chips = S.loadSavedChips();
        let html = '';
        for (const chipId of chips) {
            const short = chipId.split(':').pop();
            html += `<span class="saved-chip" data-chip="${chipId.replace(/"/g,'&quot;')}" title="${h(chipId)}">
                <span class="saved-chip-name">${h(short)}</span>
                <span class="del-chip" data-del="${chipId.replace(/"/g,'&quot;')}" title="Remove">×</span>
            </span>`;
        }
        // Preserve existing event handlers on the #saved-chips parent via delegation
        this.savedChipsEl.innerHTML = html;

        // Bind click on chip name to select
        this.savedChipsEl.querySelectorAll('.saved-chip').forEach(el => {
            el.addEventListener('click', (e) => {
                if (e.target.classList.contains('del-chip')) return;
                this.handlePaletteClick(el.dataset.chip);
            });
        });
        // Bind delete
        this.savedChipsEl.querySelectorAll('.del-chip').forEach(el => {
            el.addEventListener('click', (e) => {
                e.stopPropagation();
                const cid = el.dataset.del;
                S.removeCustomChip(cid);
                if (S.selectedChip === cid) this.setSelected('');
                this.renderSavedChips();
                Grid.render();
                this.updateRecipePanel();
                this.updateStatus();
            });
        });
        this._refreshPaletteRefs();
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

        // Build a map: nodeId → [{node, isProcess}]
        const children = new Map();
        const allNodes = new Map();
        children.set('O', []);
        allNodes.set('O', { id: 'O', label: (r.output.item || []).map(o => o.id || JSON.stringify(o)).join(','), type: 'output' });

        for (const p of r.process) {
            allNodes.set(p.id, { id: p.id, label: p.item.map(c => c.chip ? c.chip.split(':').pop() : (c.tag ? '#' + c.tag : '?')).join('+'), type: 'process' });
            if (!children.has(p.next)) children.set(p.next, []);
            children.get(p.next).push({ node: p.id, isProcess: true });
        }
        for (const inp of r.input) {
            allNodes.set(inp.id, { id: inp.id, label: typeof inp.item === 'string' ? inp.item : (inp.item.item || inp.item.tag || '?'), type: 'input' });
            if (!children.has(inp.next)) children.set(inp.next, []);
            children.get(inp.next).push({ node: inp.id, isProcess: false });
        }

        // Render tree from output (root) downward
        let html = '';
        function renderNode(nodeId, depth) {
            const info = allNodes.get(nodeId);
            if (!info) return;
            const cls = info.type === 'input' ? 'input' : info.type === 'output' ? 'output' : 'process';
            html += `<div class="fnode ${cls}" style="margin-left:${depth * 16}px">
                <div class="fid">${h(info.id)}</div>
                <div class="fitm">${h(info.label)}</div>
            </div>`;

            const ch = children.get(nodeId) || [];
            if (ch.length > 0) {
                html += '<div style="display:flex;flex-direction:column;gap:2px;">';
                for (const c of ch) {
                    html += '<div style="display:flex;align-items:flex-start;">';
                    html += `<div class="farr" style="font-size:0.9em;">←</div>`;
                    renderNode(c.node, depth + 1);
                    html += '</div>';
                }
                html += '</div>';
            }
        }
        renderNode('O', 0);

        this.recipeFlow.innerHTML = html || '<span class="dim">No recipe.</span>';
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

function h(s) {
    if (s == null) return '';
    const div = document.createElement('div');
    div.textContent = String(s);
    return div.innerHTML;
}
