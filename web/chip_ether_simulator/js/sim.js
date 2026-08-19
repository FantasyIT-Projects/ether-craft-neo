/* sim.js — Ether Craft 芯片以太模拟器
 *
 * 实现设计模型（与 EtherProcessFactoryEntity 重构讨论一致）：
 *
 *  1) 维持基础曲线 base(e)：单峰回落 + 渐近
 *        t = consume,  x = e / t,  y = (x-1)/(ratio-1)
 *        e <= t : base = e * a
 *        e >  t : base = t*a * (1 + overshoot·y·exp(λ·(1-y)))
 *     峰值出现在 x = ratio 处, 峰值高度 = t*a*(1+overshoot), e→∞ 渐近 t*a
 *
 *  2) 速度倍率 p(e)：由 e/max 驱动, 单调无界（收益递减）
 *        u = e/max,  p = 1 + log2(1+u)   （离散模式取整, 最小 1）
 *
 *  3) 总维持开销 C(e) = base(e) * p(e)
 *
 *  4) 充能：机器缓存每 tick 收 rateIn；
 *     缓存 >= Σ(k·consume_i·count_i) 时一次性按比例分发一批（每颗 +k·consume）
 *
 *  5) 加工：有效速度 —— e<consume → 0（以太不足直接停止）；否则 p(e)。
 *     路径速度 pMin = min(启用芯片的有效速度)；pMin>0 时 progress += pMin，
 *     达到 maxProgress·msm 后全部芯片扣 consume 产出
 */
'use strict';

/* ============================== 模型 ============================== */

const params = {
    rateIn: 200,
    batchSize: 1,
    a: 0.02,
    k: 5,
    minReservePer: 2,
    overshoot: 1,
    ratio: 2,
    lambda: 1,
    pFunc: 'log2',
    maxProgress: 100,
    msm: 1,
    noInput: false,
    floatCalc: false,
};

let chipDefs = [
    {id: 0, name: 'heating', count: 4, consume: 50, max: 500, enabled: true},
    {id: 1, name: 'cutting', count: 3, consume: 75, max: 750, enabled: true},
];
let nextChipId = 2;
let chipE = new Map(); // id -> 当前以太（每颗）

let state = {tick: 0, buffer: 0, cache: 0, progress: 0, produced: 0};
let history = [];
let windowSize = 2000;

function getChips() {
    return chipDefs.map(c => ({...c, e: Math.floor(chipE.get(c.id) || 0)}));
}

function baseCost(e, consume) {
    const t = consume;
    const a = params.a;
    if (e <= t) return e * a;
    if (t <= 0) return 0;
    const x = e / t;
    const r = params.ratio;
    const y = (x - 1) / (r - 1);
    const h = params.overshoot * y * Math.exp(params.lambda * (1 - y));
    return t * a * (1 + h);
}

function speedMul(e, max) {
    if (max <= 0) return 1;
    const u = e / max;
    let v;
    switch (params.pFunc) {
        case 'sqrt':
            v = 1 + Math.sqrt(u);
            break;
        case 'linear':
            v = 1 + u;
            break;
        default:
            v = 1 + Math.log2(1 + u);
    }
    return Math.max(1, Math.floor(v));
}

function totalCost(e, consume, max) {
    return Math.round(baseCost(e, consume) * speedMul(e, max));
}

/* 浮点总开销：倍率仍取整（speedMul），仅最终开销不取整，仅浮点模式维持消耗使用 */
function totalCostFloat(e, consume, max) {
    return baseCost(e, consume) * speedMul(e, max);
}

/* 有效加工速度：以太不足 consume 时无法产出，速度为 0 */
function effSpeed(e, chip) {
    return e >= chip.consume ? speedMul(e, chip.max) : 0;
}

/* 每颗芯片单批配额：max(最小配额, round(k*consume))；浮点模式不取整，与模组 reservePer 一致 */
function reservePer(c) {
    const base = params.k * c.consume;
    return params.floatCalc ? Math.max(params.minReservePer, base) : Math.max(params.minReservePer, Math.round(base));
}

function minSum() {
    let s = 0;
    for (const c of chipDefs) if (c.enabled) s += reservePer(c) * c.count;
    return s;
}

/* ============================== 模拟 ============================== */

function step() {
    state.tick++;

    // 1) 外部产生 → 外部缓存
    state.buffer += params.rateIn;
    // 2) 攒够单批次 → 整批注入机器缓存（残量留在外部缓存继续攒；batchSize=1 时每 tick 全注入，退化为原均匀输入）
    const batches = Math.floor(state.buffer / params.batchSize);
    if (batches > 0) {
        state.cache += state.buffer;
        state.buffer = 0;
    }

    // 2) 批量脉冲分发（路径 B，按批次因数一次性分发）
    const ms = minSum();
    if (ms > 0) {
        const batches = Math.floor(state.cache / ms);
        if (batches > 0) {
            for (const c of chipDefs) if (c.enabled) chipE.set(c.id, (chipE.get(c.id) || 0) + reservePer(c) * batches);
            state.cache -= batches * ms;
        }
    }

    // 3) 维持消耗（仅启用芯片）
    for (const c of chipDefs) {
        if (!c.enabled) {
            chipE.set(c.id, 0);
            continue;
        }
        const e = chipE.get(c.id) || 0;
        let ne = params.floatCalc ? e - totalCostFloat(e, c.consume, c.max) : e - totalCost(e, c.consume, c.max);
        if (ne < 0) ne = 0;
        chipE.set(c.id, ne);
    }

    // 4) 加工（启用芯片一条路径, min 聚合；以太不足 consume 时直接停止）
    const cur = getChips();
    const active = cur.filter(c => c.enabled);
    let pMin = 0;
    if (active.length) {
        if (active.some(c => c.e < c.consume)) pMin = 0;        // 以太不足 consume，停止
        else pMin = Math.min(...active.map(c => speedMul(c.e, c.max)));
    }
    if (pMin > 0 && !params.noInput) {
        state.progress += pMin;
        const target = params.maxProgress * params.msm;
        if (state.progress >= target) {
            for (const c of chipDefs) if (c.enabled) chipE.set(c.id, (chipE.get(c.id) || 0) - c.consume);
            state.progress = 0;
            state.produced++;
        }
    } else {
        state.progress = 0;
    }

    // 5) 记录（禁用芯片记 0，保持数组长度稳定）
    history.push({
        t: state.tick,
        cache: state.cache,
        pMin: pMin,
        produced: state.produced,
        e: cur.map(c => c.enabled ? c.e : 0),
        p: cur.map(c => c.enabled ? effSpeed(c.e, c) : 0),
    });
    if (history.length > windowSize) history.shift();
}

function resetSim() {
    state = {tick: 0, buffer: 0, cache: 0, progress: 0, produced: 0};
    history = [];
    chipE.clear();
}

/* ============================== 图表 ============================== */

const CHIP_COLORS = ['#2ecc71', '#26c6da', '#f4a261', '#e94560', '#9b59b6', '#f1c40f', '#1abc9c', '#e84393', '#a0a0a0', '#ff9ff3'];

function minMax(arr) {
    let mn = Infinity, mx = -Infinity;
    for (let i = 0; i < arr.length; i++) {
        const v = arr[i];
        if (v < mn) mn = v;
        if (v > mx) mx = v;
    }
    return [mn, mx];
}

function niceTicks(mn, mx, n) {
    if (mn === mx) mx = mn + 1;
    const span = mx - mn;
    const step0 = span / n;
    const mag = Math.pow(10, Math.floor(Math.log10(step0)));
    const norm = step0 / mag;
    const step = (norm < 1.5 ? 1 : norm < 3 ? 2 : norm < 7 ? 5 : 10) * mag;
    const t0 = Math.floor(mn / step) * step;
    const ticks = [];
    for (let v = t0; v <= mx; v += step) ticks.push(v);
    return ticks;
}

/* series: [{name, color, data, axis:'left'|'right', dash, width}]
 * opts: { zero, xMax, xLabel, markX, markY }  xMax 用于 fn 图 (x 轴为数值) */
function drawChart(canvas, series, opts) {
    opts = opts || {};
    const dpr = window.devicePixelRatio || 1;
    const W = canvas.clientWidth, H = canvas.clientHeight;
    if (!W || !H) return;
    canvas.width = W * dpr;
    canvas.height = H * dpr;
    const ctx = canvas.getContext('2d');
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, W, H);
    ctx.font = '10px Segoe UI, Microsoft YaHei';

    const pad = {l: 54, r: 56, t: 10, b: 20};
    const pw = W - pad.l - pad.r, ph = H - pad.t - pad.b;

    // 范围
    let hasL = false, lmin = 0, lmax = 1, hasR = false, rmin = 0, rmax = 1;
    const n = series.length ? series[0].data.length : 0;
    for (const s of series) {
        if (!s.data.length) continue;
        const [mn, mx] = minMax(s.data);
        if (s.axis === 'right') {
            rmin = hasR ? Math.min(rmin, mn) : mn;
            rmax = hasR ? Math.max(rmax, mx) : mx;
            hasR = true;
        } else {
            lmin = hasL ? Math.min(lmin, mn) : mn;
            lmax = hasL ? Math.max(lmax, mx) : mx;
            hasL = true;
        }
    }
    if (opts.zero) {
        lmin = 0;
        if (hasR) rmin = 0;
    }
    if (opts.minLeft != null) lmin = opts.minLeft;
    if (!hasL) {
        lmin = 0;
        lmax = 1;
    }
    if (!hasR) {
        rmin = 0;
        rmax = 1;
    }
    if (lmin === lmax) lmax = lmin + 1;
    if (rmin === rmax) rmax = rmin + 1;

    const X = i => pad.l + (n <= 1 ? 0 : (i / (n - 1)) * pw);
    const xVal = opts.xMax ? (v => pad.l + (v / opts.xMax) * pw) : X;
    const YL = v => pad.t + ph * (1 - (v - lmin) / (lmax - lmin));
    const YR = v => pad.t + ph * (1 - (v - rmin) / (rmax - rmin));

    // 网格 + 左轴
    ctx.strokeStyle = '#2a2a4a';
    ctx.fillStyle = '#a0a0a0';
    ctx.lineWidth = 1;
    for (const v of niceTicks(lmin, lmax, 5)) {
        const y = YL(v);
        ctx.beginPath();
        ctx.moveTo(pad.l, y);
        ctx.lineTo(W - pad.r, y);
        ctx.stroke();
        ctx.fillText(fmt(v), 2, y + 3);
    }
    // 右轴
    if (hasR) {
        for (const v of niceTicks(rmin, rmax, 4)) {
            const y = YR(v);
            ctx.fillText(fmt(v), W - pad.r + 4, y + 3);
        }
    }
    // x 轴刻度
    if (opts.xMax) {
        for (const v of niceTicks(0, opts.xMax, 5)) {
            const x = xVal(v);
            ctx.fillText(fmt(v), x - 12, H - 6);
        }
    } else if (n > 1) {
        ctx.fillText(String(history.length ? (history[history.length - 1].t - windowSize + 1) : 0), pad.l - 10, H - 6);
        ctx.fillText(String(history.length ? history[history.length - 1].t : 0), W - pad.r - 24, H - 6);
    }

    // 标记点（fn 图峰值）
    if (opts.markX != null) {
        const x = xVal(opts.markX);
        ctx.strokeStyle = '#888';
        ctx.setLineDash([4, 4]);
        ctx.beginPath();
        ctx.moveTo(x, pad.t);
        ctx.lineTo(x, pad.t + ph);
        ctx.stroke();
        ctx.setLineDash([]);
        ctx.fillStyle = '#ccc';
        ctx.fillText('峰值 ' + opts.markX.toFixed(0), x - 30, pad.t - 2);
    }

    // 数据
    for (const s of series) {
        if (!s.data.length) continue;
        ctx.strokeStyle = s.color;
        ctx.lineWidth = s.width || 1.5;
        if (s.dash) ctx.setLineDash(s.dash);
        ctx.beginPath();
        for (let i = 0; i < s.data.length; i++) {
            const x = opts.xMax ? xVal(s.xvals ? s.xvals[i] : (s.data.length <= 1 ? 0 : opts.xMax * i / (s.data.length - 1))) : X(i);
            const y = s.axis === 'right' ? YR(s.data[i]) : YL(s.data[i]);
            if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
        }
        ctx.stroke();
        ctx.setLineDash([]);
    }
    if (!n) {
        ctx.fillStyle = '#666';
        ctx.fillText('运行中…（按 播放 开始）', W / 2 - 60, H / 2);
    }
}

function fmt(v) {
    if (Math.abs(v) >= 10000) return (v / 1000).toFixed(1) + 'k';
    if (Math.abs(v) >= 100) return v.toFixed(0);
    if (Math.abs(v) >= 10) return v.toFixed(1);
    return v.toFixed(2);
}

/* ============================== 渲染 ============================== */

function setLegend(canvasId, items) {
    const canvas = document.getElementById(canvasId);
    const wrap = canvas ? canvas.closest('.chart-wrap') : null;
    if (!wrap) return;
    const el = wrap.querySelector('.legend');
    el.innerHTML = '';
    for (const it of items) {
        const span = document.createElement('span');
        const sw = document.createElement('span');
        sw.className = 'sw';
        sw.style.background = it.color;
        span.appendChild(sw);
        span.appendChild(document.createTextNode(it.name));
        el.appendChild(span);
    }
}

function renderCharts() {
    const chips = getChips();
    const active = chips.filter(c => c.enabled);
    const idxOf = id => chips.findIndex(c => c.id === id);

    // 以太图（主轴: 每类型每颗 e；副轴: 缓存）
    const sE = active.map((c, i) => ({
        name: c.name,
        color: CHIP_COLORS[i % CHIP_COLORS.length],
        data: history.map(h => h.e[idxOf(c.id)] || 0),
        axis: 'left',
    }));
    sE.push({name: '机器缓存', color: '#e0e0e0', data: history.map(h => h.cache), axis: 'right', dash: [5, 3]});
    drawChart(document.getElementById('chart-ether'), sE, {zero: true});
    setLegend('chart-ether', sE.map(s => ({name: s.name, color: s.color})));

    // 速度图（有效加工速度：e < consume 时为 0）
    const sP = active.map((c, i) => ({
        name: c.name + ' 速度',
        color: CHIP_COLORS[i % CHIP_COLORS.length],
        data: history.map(h => h.p[idxOf(c.id)] || 0),
        axis: 'left',
    }));
    sP.push({name: 'pMin(路径速度)', color: '#e94560', data: history.map(h => h.pMin), axis: 'left', width: 2});
    drawChart(document.getElementById('chart-speed'), sP, {zero: true});
    setLegend('chart-speed', sP.map(s => ({name: s.name, color: s.color})));

    // 函数曲线（参考类型）
    const refSel = document.getElementById('fn-chip-select');
    const refId = Number(refSel.value);
    const ref = chips.find(c => c.id === refId) || chips[0];
    if (ref) {
        const xMax = Math.max(ref.consume * 20, ref.max * 3, 100);
        const N = 240;
        const xs = [], bd = [], sp = [], es = [], tc = [];
        for (let i = 0; i <= N; i++) {
            const e = (xMax * i) / N;
            xs.push(e);
            bd.push(baseCost(e, ref.consume));
            const rawP = speedMul(e, ref.max);
            sp.push(rawP);
            es.push(e >= ref.consume ? rawP : 0);
            tc.push(params.floatCalc ? totalCostFloat(e, ref.consume, ref.max) : totalCost(e, ref.consume, ref.max));
        }
        const sFn = [
            {name: 'base(e)', color: '#26c6da', data: bd, xvals: xs},
            {name: 'p(e) 倍率', color: '#f1c40f', data: sp, xvals: xs, dash: [4, 3]},
            {name: '有效速度(e<consume=0)', color: '#f1c40f', data: es, xvals: xs},
            {name: 'C(e)=base·p', color: '#e94560', data: tc, xvals: xs, width: 2},
        ];
        drawChart(document.getElementById('chart-fn'), sFn, {xMax: xMax, markX: ref.consume});
        setLegend('chart-fn', sFn.map(s => ({name: s.name, color: s.color})));
    }
}

function renderStats() {
    const chips = getChips();
    const active = chips.filter(c => c.enabled);
    const cur = history.length ? history[history.length - 1] : null;
    const totalEther = active.reduce((s, c) => s + c.e * c.count, 0);
    const maintRate = active.reduce((s, c) => s + (params.floatCalc ? totalCostFloat(c.e, c.consume, c.max) : totalCost(c.e, c.consume, c.max)) * c.count, 0);
    const pMin = cur ? cur.pMin : 0;
    const craftRate = (pMin > 0 && active.length) ? (pMin / (params.maxProgress * params.msm)) : 0; // 期望加工次数/tick
    const craftCost = active.reduce((s, c) => s + c.consume * c.count, 0);
    const ref = active[0];

    // 机器状态
    let statusTxt, statusColor;
    if (!active.length) {
        statusTxt = '无芯片';
        statusColor = '#a0a0a0';
    } else if (pMin === 0) {
        statusTxt = '待机（不足 consume）';
        statusColor = '#f4a261';
    } else {
        statusTxt = '运行';
        statusColor = '#2ecc71';
    }

    let html = `
        <span class="stat">tick <b>${state.tick}</b></span>
        <span class="stat">机器状态 <b style="color:${statusColor}">${statusTxt}</b></span>
        <span class="stat">机器缓存 <b>${fmt(state.cache)}</b></span>
        <span class="stat">外部缓存 <b>${fmt(state.buffer)}</b></span>
        <span class="stat">总芯片以太 <b>${fmt(totalEther)}</b></span>
        <span class="stat">pMin <b>${pMin.toFixed(2)}</b></span>
        <span class="stat">产出 <b>${state.produced}</b></span>
        <span class="stat">维持消耗/tick <b>${fmt(maintRate)}</b></span>
        <span class="stat">加工期望/tick <b>${craftRate.toFixed(3)}</b></span>
        <span class="stat">单次加工全芯片扣款 <b>${fmt(craftCost)}</b></span>`;
    if (ref) {
        if (pMin > 0) {
            const est = ref.consume + baseCost(ref.e, ref.consume) * params.maxProgress * params.msm;
            html += `<span class="stat">单配方总开销(参考:${ref.name}) <b>≈${fmt(est)}</b><span class="dim"> = consume + base·MAX·msm</span></span>`;
        } else {
            html += `<span class="stat">单配方总开销(参考:${ref.name}) <b>—</b><span class="dim">（无产出）</span></span>`;
        }
    }
    document.getElementById('stats').innerHTML = html;

    // 芯片行实时值
    chips.forEach(c => {
        const row = document.querySelector(`.chip-row[data-id="${c.id}"]`);
        if (row) {
            row.querySelector('.chip-live').textContent =
                `e=${fmt(c.e)}  base=${fmt(baseCost(c.e, c.consume))}  p=${effSpeed(c.e, c).toFixed(2)}  C=${fmt(params.floatCalc ? totalCostFloat(c.e, c.consume, c.max) : totalCost(c.e, c.consume, c.max))}  total=${fmt(c.e * c.count)}`;
        }
    });
}

/* ============================== 循环 ============================== */

let timer = null;
let playing = false;

function setPlaying(p) {
    playing = p;
    document.getElementById('btn-play').textContent = p ? '⏸ 暂停' : '▶ 播放';
    if (p) startTimer(); else stopTimer();
}

function stopTimer() {
    if (timer) {
        clearInterval(timer);
        timer = null;
    }
}

function startTimer() {
    stopTimer();
    const tps = Number(document.getElementById('tick-per-sec').value);
    timer = setInterval(() => {
        step();
        renderCharts();
        renderStats();
    }, 1000 / tps);
}

/* ============================== 本地持久化 ============================== */

const STORE_KEY = 'etherCraftChipSimConfig';

function saveConfig() {
    const cfg = {
        params: {...params},
        chips: chipDefs.map(c => ({...c})),
        windowSize: windowSize,
        tps: Number(document.getElementById('tick-per-sec').value) || 60,
    };
    try {
        localStorage.setItem(STORE_KEY, JSON.stringify(cfg));
    } catch (e) {
        console.warn('save config failed', e);
    }
}

function loadConfig() {
    try {
        const raw = localStorage.getItem(STORE_KEY);
        if (!raw) return;
        const cfg = JSON.parse(raw);
        if (cfg.params) Object.assign(params, cfg.params);
        if (Array.isArray(cfg.chips) && cfg.chips.length) {
            chipDefs = cfg.chips.map(c => ({enabled: true, ...c}));
            nextChipId = Math.max(...chipDefs.map(c => c.id)) + 1;
        }
        if (cfg.windowSize) windowSize = cfg.windowSize;
        if (cfg.tps) document.getElementById('tick-per-sec').value = cfg.tps;
    } catch (e) {
        console.warn('load config failed', e);
    }
}

function applyParamsToUI() {
    const setNum = (id, v) => {
        const el = document.getElementById(id);
        if (el) el.value = v;
    };
    setNum('rate-in', params.rateIn);
    setNum('batch-size', params.batchSize);
    setNum('min-reserve-per', params.minReservePer);
    setNum('p-a', params.a);
    syncRangeVal('p-a', 'p-a-val', 3);
    setNum('p-k', params.k);
    syncRangeVal('p-k', 'p-k-val', 1);
    setNum('p-ov', params.overshoot);
    syncRangeVal('p-ov', 'p-ov-val', 2);
    setNum('p-ratio', params.ratio);
    syncRangeVal('p-ratio', 'p-ratio-val', 1);
    setNum('p-lambda', params.lambda);
    syncRangeVal('p-lambda', 'p-lambda-val', 2);
    document.getElementById('p-func').value = params.pFunc;
    document.getElementById('no-input').checked = !!params.noInput;
    document.getElementById('float-calc').checked = !!params.floatCalc;
    setNum('p-maxprogress', params.maxProgress);
    setNum('p-msm', params.msm);
    setNum('window-size', windowSize);
    syncRangeVal('tick-per-sec', 'tick-per-sec-val', 0);
}

/* ============================== UI 绑定 ============================== */

function bindInput(id, setter) {
    const el = document.getElementById(id);
    el.addEventListener('input', () => setter(el));
    el.addEventListener('change', () => setter(el));
}

function readParams() {
    params.rateIn = Number(document.getElementById('rate-in').value) || 0;
    params.batchSize = Math.max(1, Number(document.getElementById('batch-size').value) || 1);
    params.minReservePer = Math.max(0, Number(document.getElementById('min-reserve-per').value) || 2);
    params.a = Number(document.getElementById('p-a').value);
    params.k = Number(document.getElementById('p-k').value);
    params.overshoot = Number(document.getElementById('p-ov').value);
    params.ratio = Number(document.getElementById('p-ratio').value) || 2;
    params.lambda = Number(document.getElementById('p-lambda').value);
    params.pFunc = document.getElementById('p-func').value;
    params.maxProgress = Number(document.getElementById('p-maxprogress').value) || 1;
    params.msm = Number(document.getElementById('p-msm').value) || 1;
    params.noInput = document.getElementById('no-input').checked;
    params.floatCalc = document.getElementById('float-calc').checked;
    windowSize = Number(document.getElementById('window-size').value) || 2000;
    saveConfig();
}

function syncRangeVal(id, valId, digits) {
    const el = document.getElementById(id);
    const v = Number(el.value);
    document.getElementById(valId).textContent = v.toFixed(digits);
    return v;
}

function readChipsFromUI() {
    const rows = document.querySelectorAll('.chip-row');
    const next = [];
    for (const row of rows) {
        const id = Number(row.dataset.id);
        const g = sel => row.querySelector(sel);
        const enabled = g('.c-enabled').checked;
        const c = {
            id: id,
            name: g('.c-name').value.trim() || ('chip' + id),
            count: Math.max(0, Number(g('.c-count').value) || 0),
            consume: Math.max(0, Number(g('.c-consume').value) || 0),
            max: Math.max(0, Number(g('.c-max').value) || 1),
            enabled: enabled,
        };
        next.push(c);
        // 禁用 = 移出机器（中途加入从 0 以太开始）
        if (!enabled) chipE.set(id, 0);
        else if (!chipE.has(id)) chipE.set(id, 0);
    }
    chipDefs = next;
    buildFnSelect();
    saveConfig();
}

function buildChipRows() {
    const box = document.getElementById('chip-rows');
    box.innerHTML = '';
    for (const c of chipDefs) {
        const row = document.createElement('div');
        row.className = 'chip-row' + (c.enabled === false ? ' disabled' : '');
        row.dataset.id = c.id;
        row.innerHTML = `
            <div class="chip-head">
                <input type="text" class="c-name" value="${c.name}">
                <label class="c-enable-label"><input type="checkbox" class="c-enabled" ${c.enabled === false ? '' : 'checked'}>启用</label>
                <button class="btn small del">删</button>
            </div>
            <div class="chip-fields">
                <label>数量<input type="number" class="c-count" min="0" step="1" value="${c.count}"></label>
                <label>consume<input type="number" class="c-consume" min="0" step="1" value="${c.consume}"></label>
                <label>标准存量<input type="number" class="c-max" min="1" step="1" value="${c.max}"></label>
            </div>
            <div class="chip-live"></div>`;
        box.appendChild(row);
    }
    buildFnSelect();
}

function buildFnSelect() {
    const sel = document.getElementById('fn-chip-select');
    const prev = sel.value;
    sel.innerHTML = '';
    for (const c of chipDefs) {
        const opt = document.createElement('option');
        opt.value = c.id;
        opt.textContent = c.name;
        sel.appendChild(opt);
    }
    if (chipDefs.some(c => String(c.id) === prev)) sel.value = prev;
}

function init() {
    loadConfig();
    applyParamsToUI();

    bindInput('tick-per-sec', el => {
        syncRangeVal('tick-per-sec', 'tick-per-sec-val', 0);
        saveConfig();
        if (playing) startTimer();
    });
    bindInput('p-a', el => {
        syncRangeVal('p-a', 'p-a-val', 3);
        readParams();
    });
    bindInput('p-k', el => {
        syncRangeVal('p-k', 'p-k-val', 1);
        readParams();
    });
    bindInput('p-ov', el => {
        syncRangeVal('p-ov', 'p-ov-val', 2);
        readParams();
    });
    bindInput('p-ratio', el => {
        syncRangeVal('p-ratio', 'p-ratio-val', 1);
        readParams();
    });
    bindInput('p-lambda', el => {
        syncRangeVal('p-lambda', 'p-lambda-val', 2);
        readParams();
    });
    bindInput('p-func', el => readParams());
    bindInput('no-input', el => readParams());
    bindInput('float-calc', el => readParams());
    bindInput('p-maxprogress', el => readParams());
    bindInput('p-msm', el => readParams());
    bindInput('rate-in', el => readParams());
    bindInput('batch-size', el => readParams());
    bindInput('min-reserve-per', el => readParams());
    bindInput('window-size', el => readParams());

    // 播放/暂停/单步/重置
    document.getElementById('btn-play').addEventListener('click', () => setPlaying(!playing));
    document.getElementById('btn-step').addEventListener('click', () => {
        setPlaying(false);
        step();
        renderCharts();
        renderStats();
    });
    document.getElementById('btn-reset').addEventListener('click', () => {
        resetSim();
        setPlaying(false);
        renderCharts();
        renderStats();
    });

    // 添加芯片
    document.getElementById('btn-add-chip').addEventListener('click', () => {
        chipDefs.push({id: nextChipId++, name: 'chip' + nextChipId, count: 1, consume: 50, max: 500, enabled: true});
        chipE.set(chipDefs[chipDefs.length - 1].id, 0);
        buildChipRows();
        readChipsFromUI();
        resetSim();
        setPlaying(false);
        renderCharts();
        renderStats();
    });

    // 芯片行变更（事件委托：参数、启用开关）
    document.getElementById('chip-rows').addEventListener('input', () => readChipsFromUI());
    document.getElementById('chip-rows').addEventListener('change', () => readChipsFromUI());
    document.getElementById('chip-rows').addEventListener('click', (ev) => {
        const btn = ev.target.closest('.del');
        if (!btn) return;
        const row = btn.closest('.chip-row');
        const id = Number(row.dataset.id);
        chipE.delete(id);
        chipDefs = chipDefs.filter(c => c.id !== id);
        buildChipRows();
        readChipsFromUI();
        resetSim();
        setPlaying(false);
        renderCharts();
        renderStats();
    });

    document.getElementById('fn-chip-select').addEventListener('change', () => renderCharts());

    readParams();
    buildChipRows();
    readChipsFromUI();
    resetSim();
    renderCharts();
    renderStats();
    setPlaying(true);
}

document.addEventListener('DOMContentLoaded', init);
