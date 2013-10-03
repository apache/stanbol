package org.apache.stanbol.enhancer.engine.disambiguation.freebase.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;

/**
 * <p>
 * Custom implementation of GraphJung which allows the graph to be undirected, i.e, not taking into account
 * the direction of the edges
 * </p>
 * 
 * @author Antonio David Perez Morales <adperezmorales@gmail.com>
 * 
 */
public class UndirectedGraphJung extends GraphJung<Graph> {

    /**
     * <p>
     * Default constructor
     * </p>
     * 
     * @param graph
     *            The {@code Graph} object to be used as raw graph for JUNG implementation
     */
    public UndirectedGraphJung(Graph graph) {
        super(graph);
    }

    /**
     * <p>
     * Get the out edges for a vertex
     * </p>
     * <p>
     * Returns the incoming and outgoing edges for the vertex
     * </p>
     * 
     * @param vertex
     *            The vertex
     * @return The {@code Collection<Edge>} of edges
     */
    public Collection<Edge> getOutEdges(final Vertex vertex) {
        return getEdgesForVertex(vertex);
    }

    /**
     * <p>
     * Get the in edges for a vertex
     * </p>
     * <p>
     * Returns the incoming and outgoing edges for the vertex
     * </p>
     * 
     * @param vertex
     *            The vertex
     * @return The {@code Collection<Edge>} of edges
     */
    public Collection<Edge> getInEdges(Vertex vertex) {
        return getEdgesForVertex(vertex);
    }

    /**
     * <p>
     * Gets all the edges for a vertex, both incoming and outgoing edges
     * </p>
     * 
     * @param vertex
     *            The vertex
     * @return The {@code Collection<Edge>} of edges
     * 
     */
    private Collection<Edge> getEdgesForVertex(Vertex vertex) {
        final Iterable<Edge> itty = vertex.getEdges(Direction.BOTH);
        if (itty instanceof Collection) {
            return (Collection<Edge>) itty;
        } else {
            final List<Edge> edges = new ArrayList<Edge>();
            for (final Edge edge : itty) {
                edges.add(edge);
            }
            return edges;
        }
    }

}
