package studio.fantasyit.ether_craft.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeLike<T,U> {
    public static class TreeNode<T,U> {
        public T value;
        public Integer id;
        public List<TreeEdge<T,U>> edges;

        public TreeNode(Integer id,T value) {
            this.id = id;
            this.value = value;
            edges = new ArrayList<>();
        }
    }
    public static class TreeEdge<T,U> {
        public U value;
        public TreeNode<T,U> node;
        public TreeNode<T,U> from;

        public TreeEdge(U value, TreeNode<T,U> node, TreeNode<T,U> from) {
            this.value = value;
            this.node = node;
            this.from = from;
        }
    }
    private final TreeNode<T,U> root;
    private final Map<Integer,TreeNode<T,U>> idMapping;
    private final List<TreeEdge<T,U>> edges;
    private Integer maxId = 0;

    public TreeLike(Integer id,T value) {
        root = new TreeNode<>(id,value);
        edges = new ArrayList<>();
        idMapping = new HashMap<>();
        idMapping.put(id,root);
    }
    public TreeNode<T, U> addNode(Integer id,T value) {
        maxId = Math.max(maxId,id);
        idMapping.put(id,new TreeNode<>(id,value));
        return idMapping.get(id);
    }

    public TreeLike<T,U> addEdge(Integer from, Integer to, U value) {
        TreeNode<T,U> fromNode = idMapping.get(from);
        TreeNode<T,U> toNode = idMapping.get(to);
        TreeEdge<T, U> tuTreeEdge = new TreeEdge<>(value, toNode, fromNode);
        edges.add(tuTreeEdge);
        fromNode.edges.add(tuTreeEdge);
        return this;
    }

    public TreeNode<T, U> getRoot() {
        return root;
    }
    public List<TreeEdge<T, U>> getEdge(Integer id){
        return getNode(id).edges;
    }
    public List<TreeEdge<T, U>> getEdges() {
        return edges;
    }
    public List<TreeNode<T, U>> getNodes() {
        return new ArrayList<>(idMapping.values());
    }
    public TreeNode<T,U> getNode(Integer value) {
        return idMapping.get(value);
    }
    public Integer getMaxId() {
        return maxId;
    }
}
