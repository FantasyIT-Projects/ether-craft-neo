// grid.js — 9×9 grid rendering and interaction

const Grid = {

    _gridEl: null,

    init(containerEl) {
        this._gridEl = containerEl;
    },

    render() {
        if (!this._gridEl) return;
        this._gridEl.innerHTML = '';

        for (let y = 0; y < S.ROWS; y++) {
            for (let x = 0; x < S.COLS; x++) {
                const cellData = S.grid[y][x];
                const cell = document.createElement('div');
                cell.className = 'g-cell';
                cell.dataset.x = x;
                cell.dataset.y = y;

                // Type-specific styling
                if (cellData.type === 'chip' && cellData.chipId) {
                    const info = S.chipInfo(cellData.chipId);
                    if (info) {
                        cell.classList.add('chip', info.cls);
                        cell.style.background = info.color;
                        cell.textContent = info.label;
                    } else {
                        cell.classList.add('chip', 'chip-custom');
                        cell.textContent = cellData.chipId.split(':').pop();
                    }
                } else if (cellData.type === 'block') {
                    cell.classList.add('blocked');
                    cell.textContent = '█';
                }

                // Mark overlay (from detection)
                if (S.markMatrix) {
                    const v = S.markMatrix[y][x];
                    if (v > 0 && v !== 100) {
                        cell.classList.add('m');
                    }
                }

                // Path overlay
                if (S.detectedRecipe && S.detectedRecipe.pathCells) {
                    if (S.detectedRecipe.pathCells.has(`${x},${y}`)) {
                        cell.classList.add('path');
                    }
                    if (S.detectedRecipe.chipCells && S.detectedRecipe.chipCells.has(`${x},${y}`)) {
                        cell.classList.add('chip-active');
                    }
                }

                // Events
                cell.addEventListener('click', () => this.handleClick(x, y));
                cell.addEventListener('contextmenu', (e) => {
                    e.preventDefault();
                    this.handleRightClick(x, y);
                });

                // Hover highlight: show which chips feed the current path cell
                cell.addEventListener('mouseenter', () => {
                    if (S.detectedRecipe && S.detectedRecipe.pathCells) {
                        if (S.detectedRecipe.pathCells.has(`${x},${y}`)) {
                            for (const [dx, dy] of S.DIRS) {
                                const nx = x + dx, ny = y + dy;
                                if (nx >= 0 && nx < S.COLS && ny >= 0 && ny < S.ROWS) {
                                    const key = `${nx},${ny}`;
                                    if (S.detectedRecipe.chipCells && S.detectedRecipe.chipCells.has(key)) {
                                        const nb = this._gridEl.querySelector(`[data-x="${nx}"][data-y="${ny}"]`);
                                        if (nb) nb.style.outline = '2px solid #fff';
                                    }
                                }
                            }
                        }
                    }
                });
                cell.addEventListener('mouseleave', () => {
                    this._gridEl.querySelectorAll('.g-cell').forEach(c => c.style.outline = '');
                });

                this._gridEl.appendChild(cell);
            }
        }
    },

    handleClick(x, y) {
        const sel = S.selectedChip;
        if (sel === '') {
            S.grid[y][x] = { type: 'empty', chipId: null };
        } else if (sel === 'block') {
            S.grid[y][x] = { type: 'block', chipId: null };
        } else {
            S.grid[y][x] = { type: 'chip', chipId: sel };
        }
        S.clearDetection();
        this.render();
        UI.updateRecipePanel();
        UI.updateStatus();
        if (typeof scheduleAutoDetect === 'function') scheduleAutoDetect();
    },

    handleRightClick(x, y) {
        S.grid[y][x] = { type: 'empty', chipId: null };
        S.clearDetection();
        this.render();
        UI.updateRecipePanel();
        UI.updateStatus();
        if (typeof scheduleAutoDetect === 'function') scheduleAutoDetect();
    },

    clearAll() {
        S.initGrid();
        S.clearDetection();
        this.render();
        UI.updateRecipePanel();
        UI.updateStatus();
    },
};
