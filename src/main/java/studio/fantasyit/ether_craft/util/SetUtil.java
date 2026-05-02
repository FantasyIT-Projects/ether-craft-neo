package studio.fantasyit.ether_craft.util;


import studio.fantasyit.ether_craft.base.GraphLike;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class SetUtil {
    /**
     * 该方法用于判断两个集合排列，能否找出第三个排列，使得每个数字都同时出现在对应位置的两个集合中
     * @param sets1
     * @param sets2
     * @return
     */
    public static boolean setVeirfy(List<Set<Integer>> sets1, List<Set<Integer>> sets2) {
        if(sets1.size()!=sets2.size())return false;
        for(int i=0;i<sets1.size();i++){
            if(sets1.get(i).isEmpty() || sets2.get(i).isEmpty())return false;
        }
        GraphLike<Integer> graph = new GraphLike<>();
        int baseId = sets1.size();
        for(int i=0;i<sets1.size();i++)graph.addNode(i);
        for(int i=0;i<baseId;i++)graph.addNode(baseId+i);

        for (int i = 0; i < sets1.size(); i++) {
            Set<Integer> set1 = sets1.get(i);
            Set<Integer> set2 = sets2.get(i);
            for (Integer integer : set1) {
                if(!set2.contains(integer)){
                    GraphLike.GraphNode<Integer> node = graph.getNode(i);
                    GraphLike.GraphNode<Integer> node1 = graph.getNode(integer + baseId);
                    graph.addEdgeX(node,node1);
                }
            }
        }

        return biPartiteGraphMatchFull(graph);
    }
    private static boolean biPartiteGraphMatchFull(GraphLike<Integer> graph){
        int[] a2b = new int[graph.getNodes().size() / 2];
        Arrays.fill(a2b, -1);
        return biPartiteGraphMatch(graph,a2b)==graph.getNodes().size()/2;
    }
    public static int[] biPartiteGraphMatchGetResult(GraphLike<Integer> graph) {
        int[] a2b = new int[graph.getNodes().size() / 2];
        Arrays.fill(a2b, -1);
        biPartiteGraphMatch(graph,a2b);
        return a2b;
    }
    private static int biPartiteGraphMatch(GraphLike<Integer> graph,int[] b2a) {
        boolean[] vis = new boolean[b2a.length];

        for(int i=0;i<b2a.length;i++){
            for(int j=0;j<b2a.length;j++)vis[j]=false;
            for(GraphLike.GraphEdge<Integer> edge:graph.getEdge(i)){
                int j = edge.to.value - graph.getNodes().size() / 2;
                if(_biPartiteGraphMatchFind(graph,b2a,j,vis)){
                    b2a[i]=j;
                    break;
                }
            }
        }
        int count = b2a.length;
        for(int i=0;i<b2a.length;i++){
            if(b2a[i]!=-1)count--;
        }
        return count;
    }
    private static boolean _biPartiteGraphMatchFind(GraphLike<Integer> graph,int[] b2a,int j,boolean[] vis){
        if(b2a[j] == -1)return true;
        vis[j] = true;
        int i = b2a[j];
        for(GraphLike.GraphEdge<Integer> edge:graph.getEdge(i+graph.getNodes().size() / 2)){
            int e = edge.to.value - graph.getNodes().size() / 2;
            if(!vis[e] && _biPartiteGraphMatchFind(graph,b2a,e,vis)){
                b2a[e] = i;
                return true;
            }
        }
        return false;
    }
}
