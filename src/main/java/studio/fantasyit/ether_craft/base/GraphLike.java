package studio.fantasyit.ether_craft.base;

import java.util.ArrayList;
import java.util.List;

public class GraphLike<T> {
    public static class GraphNode<T>{
        public T value;
        public List<GraphEdge<T>> edges;
        public GraphNode(T value)
        {
            this.value=value;
            edges=new ArrayList<>();
        }
    }
    public static class GraphEdge<T> {
        public GraphNode<T> from;
        public GraphNode<T> to;
        public GraphEdge(GraphNode<T> from,GraphNode<T> to){
            this.from=from;
            this.to=to;
        }
    }
    List<GraphNode<T>> nodes;
    public GraphLike(){
        nodes=new ArrayList<>();
    }

    public int addNode(T value){
        GraphNode<T> node=new GraphNode<>(value);
        nodes.add(node);
        return nodes.size()-1;
    }
    public void addEdge(GraphNode<T> from,GraphNode<T> to){
        GraphEdge<T> edge=new GraphEdge<>(from,to);
        from.edges.add(edge);
    }
    public void addEdgeX(GraphNode<T> from,GraphNode<T> to){
        addEdge(from,to);
        addEdge(to,from);
    }
    public List<GraphNode<T>> getNodes(){
        return nodes;
    }
    public List<GraphEdge<T>> getEdge(int id) {
        return getNode(id).edges;
    }
    public GraphNode<T> getNode(int id){
        return nodes.get(id);
    }
}
