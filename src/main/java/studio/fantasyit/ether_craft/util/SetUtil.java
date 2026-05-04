package studio.fantasyit.ether_craft.util;


import studio.fantasyit.ether_craft.base.GraphLike;

import java.util.*;

public class SetUtil {

    /**
     * 判断是否可以给每个位置 i 分配一个数字 x，满足：
     * - x ∈ sets1[i] ∩ sets2[i]
     * - 所有分配的数字互不相同（形成完美匹配）
     */
    public static boolean setVeirfy(List<Set<Integer>> sets1, List<Set<Integer>> sets2) {
        if (sets1.size() != sets2.size()) return false;
        int n = sets1.size();

        // 1. 收集所有交集数字，建立数字 -> 右侧节点ID的映射
        Map<Integer, Integer> numToRightId = new HashMap<>();
        for (int i = 0; i < n; i++) {
            for (int x : sets1.get(i)) {
                if (sets2.get(i).contains(x)) {
                    numToRightId.putIfAbsent(x, numToRightId.size());
                }
            }
        }
        int m = numToRightId.size();
        if (m < n) return false;          // 数字种类不足，不可能完美匹配

        // 2. 构建二分图：左侧 [0, n-1]，右侧 [n, n+m-1]
        GraphLike<Integer> graph = new GraphLike<>();
        for (int i = 0; i < n; i++) graph.addNode(i);         // 左部节点
        for (int i = 0; i < m; i++) graph.addNode(n + i);     // 右部节点（值不重要，只是占位）

        for (int i = 0; i < n; i++) {
            Set<Integer> intersection = new HashSet<>(sets1.get(i));
            intersection.retainAll(sets2.get(i));              // 求交集
            if (intersection.isEmpty()) return false;         // 某个位置无候选数字

            GraphLike.GraphNode<Integer> leftNode = graph.getNode(i);
            for (int x : intersection) {
                int rightId = n + numToRightId.get(x);       // 右侧节点在graph中的ID
                GraphLike.GraphNode<Integer> rightNode = graph.getNode(rightId);
                graph.addEdge(leftNode, rightNode);          // 左 -> 右 单向边
            }
        }

        // 3. 匈牙利算法求最大匹配，并判断是否为完美匹配
        return biPartiteGraphMatchFull(graph, n, m);
    }

    /**
     * 判断二分图（左部节点 [0, n-1]，右部节点 [n, n+m-1]）是否存在完美匹配
     */
    private static boolean biPartiteGraphMatchFull(GraphLike<Integer> graph, int n, int m) {
        int[] matchR = new int[m];           // matchR[j] = 匹配到的左侧节点索引，-1表示未匹配
        Arrays.fill(matchR, -1);
        int matched = 0;
        for (int i = 0; i < n; i++) {
            boolean[] vis = new boolean[m];
            if (dfs(i, graph, matchR, vis)) {
                matched++;
            }
        }
        return matched == n;                 // 完美匹配 <=> 所有左侧都匹配上
    }

    /**
     * 从左侧节点 u 出发，尝试寻找增广路
     *
     * @param u      当前左侧节点索引（0..n-1）
     * @param graph  二分图
     * @param matchR 右侧匹配数组
     * @param vis    本轮搜素已访问的右侧节点
     * @return 是否找到增广路
     */
    private static boolean dfs(int u, GraphLike<Integer> graph, int[] matchR, boolean[] vis) {
        for (GraphLike.GraphEdge<Integer> edge : graph.getEdge(u)) {
            int v = edge.to.value - matchR.length;   // 右侧节点在 matchR 中的索引
            if (vis[v]) continue;
            vis[v] = true;
            // 如果右侧节点未匹配，或者其匹配的左侧节点能腾出位置
            if (matchR[v] == -1 || dfs(matchR[v], graph, matchR, vis)) {
                matchR[v] = u;
                return true;
            }
        }
        return false;
    }

    /**
     * 获取匹配结果：返回长度为 n 的数组，表示每个左侧节点匹配到的右侧节点索引（0..m-1），-1表示未匹配
     */
    public static int[] biPartiteGraphMatchGetResult(GraphLike<Integer> graph, int n, int m) {
        int[] matchR = new int[m];
        Arrays.fill(matchR, -1);
        for (int i = 0; i < n; i++) {
            boolean[] vis = new boolean[m];
            dfs(i, graph, matchR, vis);
        }
        // 转换为 left -> right
        int[] left2right = new int[n];
        Arrays.fill(left2right, -1);
        for (int j = 0; j < m; j++) {
            if (matchR[j] != -1) {
                left2right[matchR[j]] = j;
            }
        }
        return left2right;
    }
}
