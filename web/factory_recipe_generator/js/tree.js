// tree.js — TreeLike data structure used by recipe detection

class RecipeTree {
    constructor(rootId, rootValue) {
        this._nodes = new Map();
        this._edges = [];
        this.maxId = rootId;

        const root = { id: rootId, value: rootValue, edges: [] };
        this._nodes.set(rootId, root);
    }

    addNode(id, value) {
        this.maxId = Math.max(this.maxId, id);
        const node = { id, value, edges: [] };
        this._nodes.set(id, node);
        return node;
    }

    addEdge(fromId, toId, value) {
        const from = this._nodes.get(fromId);
        const to = this._nodes.get(toId);
        if (!from || !to) return null;
        const edge = { value, node: to, from };
        this._edges.push(edge);
        from.edges.push(edge);
        return edge;
    }

    getNode(id) {
        return this._nodes.get(id) || null;
    }

    getEdges(nodeId) {
        const node = this._nodes.get(nodeId);
        return node ? node.edges : [];
    }

    getRoot() {
        return this._nodes.get(0) || null;
    }

    get allEdges() {
        return this._edges;
    }

    get allNodes() {
        return Array.from(this._nodes.values());
    }

    hasNode(id) {
        return this._nodes.has(id);
    }
}

// Lightweight bipartite graph for set matching (not directly used in recognition)
class BipartiteGraph {
    constructor(nLeft, nRight) {
        this.nLeft = nLeft;
        this.nRight = nRight;
        this.adj = Array.from({ length: nLeft }, () => []);
    }

    addEdge(u, v) {
        if (u >= 0 && u < this.nLeft && v >= 0 && v < this.nRight) {
            this.adj[u].push(v);
        }
    }

    // Hungarian algorithm — returns array of length nLeft with matched right indices (-1 = unmatched)
    maximumMatching() {
        const matchR = Array(this.nRight).fill(-1);
        for (let u = 0; u < this.nLeft; u++) {
            const vis = Array(this.nRight).fill(false);
            this._dfs(u, matchR, vis);
        }
        const left2right = Array(this.nLeft).fill(-1);
        for (let v = 0; v < this.nRight; v++) {
            if (matchR[v] !== -1) {
                left2right[matchR[v]] = v;
            }
        }
        return left2right;
    }

    _dfs(u, matchR, vis) {
        for (const v of this.adj[u]) {
            if (vis[v]) continue;
            vis[v] = true;
            if (matchR[v] === -1 || this._dfs(matchR[v], matchR, vis)) {
                matchR[v] = u;
                return true;
            }
        }
        return false;
    }
}
