// exporter.js — Grid Recipe Exporter
// Converts exported 9x9 grid layout files into ether_process_factory_grid recipe JSON

const Exporter = {

    sourceHandle: null,
    targetHandle: null,
    fileEntries: [],

    init() {
        document.getElementById('btn-source-dir').addEventListener('click', () => this.pickSourceDir());
        document.getElementById('btn-target-dir').addEventListener('click', () => this.pickTargetDir());
        document.getElementById('btn-process').addEventListener('click', () => this.processAll());
        document.getElementById('select-all').addEventListener('change', (e) => this.toggleSelectAll(e.target.checked));
    },

    async pickSourceDir() {
        try {
            this.sourceHandle = await window.showDirectoryPicker();
            document.getElementById('source-path').textContent = this.sourceHandle.name;
            document.getElementById('source-path').classList.add('set');
            await this.scanSourceDir();
            this.updateProcessBtn();
        } catch (err) {
            if (err.name !== 'AbortError') {
                console.error('Failed to open source directory:', err);
            }
        }
    },

    async pickTargetDir() {
        try {
            this.targetHandle = await window.showDirectoryPicker();
            document.getElementById('target-path').textContent = this.targetHandle.name;
            document.getElementById('target-path').classList.add('set');
            this.updateProcessBtn();
        } catch (err) {
            if (err.name !== 'AbortError') {
                console.error('Failed to open target directory:', err);
            }
        }
    },

    async scanSourceDir() {
        this.fileEntries = [];
        const section = document.getElementById('file-list-section');
        const container = document.getElementById('file-list');
        const countEl = document.getElementById('file-count');

        for await (const [name, handle] of this.sourceHandle.entries()) {
            if (handle.kind === 'file' && name.toLowerCase().endsWith('.json')) {
                this.fileEntries.push({ name, handle });
            }
        }

        this.fileEntries.sort((a, b) => a.name.localeCompare(b.name));

        if (this.fileEntries.length === 0) {
            section.style.display = 'block';
            container.innerHTML = '<div class="no-files">No .json files found in this directory.</div>';
            countEl.textContent = '';
            document.getElementById('select-all').checked = false;
            return;
        }

        section.style.display = 'block';
        countEl.textContent = this.fileEntries.length + ' file(s)';

        let html = '';
        for (const entry of this.fileEntries) {
            html += '<div class="file-item">'
                + '<input type="checkbox" class="file-check" data-name="' + hesc(entry.name) + '" checked>'
                + '<span class="fname">' + hesc(entry.name) + '</span>'
                + '</div>';
        }
        container.innerHTML = html;
        document.getElementById('select-all').checked = true;
    },

    toggleSelectAll(checked) {
        document.querySelectorAll('.file-check').forEach(cb => { cb.checked = checked; });
    },

    getSelectedFiles() {
        const selected = [];
        const checkboxes = document.querySelectorAll('.file-check:checked');
        const checkedNames = new Set();
        checkboxes.forEach(cb => checkedNames.add(cb.dataset.name));

        for (const entry of this.fileEntries) {
            if (checkedNames.has(entry.name)) {
                selected.push(entry);
            }
        }
        return selected;
    },

    updateProcessBtn() {
        const btn = document.getElementById('btn-process');
        btn.disabled = !(this.sourceHandle && this.targetHandle && this.fileEntries.length > 0);
    },

    async processAll() {
        const selected = this.getSelectedFiles();
        if (selected.length === 0) {
            alert('No files selected.');
            return;
        }
        if (!this.targetHandle) {
            alert('Please select a target directory first.');
            return;
        }

        const btn = document.getElementById('btn-process');
        const progress = document.getElementById('progress-text');
        btn.disabled = true;

        const results = [];
        const resultsSection = document.getElementById('results-section');
        const resultsList = document.getElementById('results-list');

        resultsSection.style.display = 'block';
        resultsList.innerHTML = '';

        for (let i = 0; i < selected.length; i++) {
            const entry = selected[i];
            progress.textContent = 'Processing ' + (i + 1) + '/' + selected.length + ': ' + entry.name;

            try {
                const recipeJson = await this.processFile(entry);
                await this.writeToTarget(entry.name, recipeJson);
                results.push({ name: entry.name, ok: true, msg: 'Written' });
                this.markFileItem(entry.name, 'ok');
            } catch (err) {
                results.push({ name: entry.name, ok: false, msg: err.message });
                this.markFileItem(entry.name, 'err');
            }

            this.renderResults(results);
        }

        this.renderSummary(results);
        progress.textContent = 'Done.';
        btn.disabled = false;
    },

    async processFile(entry) {
        const file = await entry.handle.getFile();
        const text = await file.text();
        let data;
        try {
            data = JSON.parse(text);
        } catch (e) {
            throw new Error('Invalid JSON');
        }

        if (!data || !data.grid || !Array.isArray(data.grid)) {
            throw new Error('Not a valid grid file (missing grid array)');
        }

        const grid = data.grid;
        const rows = grid.length;
        if (rows === 0) throw new Error('Grid is empty');
        const cols = grid[0].length;
        const outputItemId = data.outputItemId || 'minecraft:air';

        // Find bounding rect of all chip cells (same algorithm as EtherProcessFactoryGrid.findRect)
        let minX = cols, minY = rows, maxX = -1, maxY = -1;
        for (let y = 0; y < rows; y++) {
            for (let x = 0; x < cols; x++) {
                const cell = grid[y][x];
                if (cell && cell.type === 'chip' && cell.chipId) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < 0) {
            throw new Error('No chip cells found in grid');
        }

        const rectWidth = maxX - minX + 1;
        const rectHeight = maxY - minY + 1;

        // Extract chip cells within the rect as ItemStackTemplates
        const recipeGrid = [];
        for (let y = minY; y <= maxY; y++) {
            const row = [];
            for (let x = minX; x <= maxX; x++) {
                const cell = grid[y][x];
                if (cell && cell.type === 'chip' && cell.chipId) {
                    row.push(this.makeChipTemplate(cell.chipId));
                } else {
                    throw new Error('Non-chip cell found inside chip bounding rect at (' + x + ',' + y + ')');
                }
            }
            recipeGrid.push(row);
        }

        return {
            type: 'ether_craft:ether_process_factory_grid',
            target: this.makeItemTemplate(outputItemId),
            grid: recipeGrid,
        };
    },

    parseItemWithCount(raw) {
        if (!raw) return { id: 'minecraft:air', count: 1 };
        const m = raw.match(/^(.+?)::(\d+)$/);
        if (m) return { id: m[1], count: parseInt(m[2], 10) };
        return { id: raw, count: 1 };
    },

    makeItemTemplate(raw) {
        const p = this.parseItemWithCount(raw);
        return { id: p.id, count: p.count };
    },

    makeChipTemplate(raw) {
        const p = this.parseItemWithCount(raw);
        const result = {
            id: 'ether_craft:process_chip',
            count: p.count,
            components: {
                'ether_craft:ether_process_chip_id': p.id,
            }
        };
        if (p.id !== 'ether_craft:separator_chip') {
            result.components['minecraft:item_model'] = p.id;
        }
        return result;
    },

    async writeToTarget(filename, recipeJson) {
        const fileHandle = await this.targetHandle.getFileHandle(filename, { create: true });
        const writable = await fileHandle.createWritable();
        try {
            await writable.write(JSON.stringify(recipeJson, null, 2));
        } finally {
            await writable.close();
        }
    },

    markFileItem(name, status) {
        const checkboxes = document.querySelectorAll('.file-check');
        checkboxes.forEach(cb => {
            if (cb.dataset.name === name) {
                const item = cb.closest('.file-item');
                if (item) {
                    const fname = item.querySelector('.fname');
                    if (fname) {
                        fname.classList.remove('result-ok', 'result-error');
                        fname.classList.add(status === 'ok' ? 'result-ok' : 'result-error');
                    }
                }
            }
        });
    },

    renderResults(results) {
        const list = document.getElementById('results-list');
        let html = '';
        for (const r of results) {
            html += '<div class="result-item ' + (r.ok ? 'ok' : 'err') + '">'
                + '<span class="rname">' + hesc(r.name) + '</span>'
                + '<span class="rmsg">' + hesc(r.msg) + '</span>'
                + '</div>';
        }
        list.innerHTML = html;
    },

    renderSummary(results) {
        const list = document.getElementById('results-list');
        const okCount = results.filter(r => r.ok).length;
        const failCount = results.filter(r => !r.ok).length;
        const total = results.length;

        let cls, text;
        if (failCount === 0) {
            cls = 'success';
            text = 'All ' + total + ' file(s) processed successfully.';
        } else if (okCount === 0) {
            cls = 'failure';
            text = 'All ' + total + ' file(s) failed.';
        } else {
            cls = 'partial';
            text = okCount + '/' + total + ' succeeded, ' + failCount + ' failed.';
        }

        const summary = '<div class="result-summary ' + cls + '">' + text + '</div>';
        list.insertAdjacentHTML('afterbegin', summary);
    },
};

function hesc(s) {
    if (s == null) return '';
    const div = document.createElement('div');
    div.textContent = String(s);
    return div.innerHTML;
}

document.addEventListener('DOMContentLoaded', () => {
    if (typeof window.showDirectoryPicker !== 'function') {
        const app = document.getElementById('app');
        app.innerHTML = '<div style="padding:40px;text-align:center;color:#e63946;">'
            + '<h2>Browser Not Supported</h2>'
            + '<p>This tool requires the <b>File System Access API</b>.</p>'
            + '<p>Please use <b>Chrome</b> or <b>Edge</b> (version 86+).</p>'
            + '</div>';
        return;
    }
    Exporter.init();
});
